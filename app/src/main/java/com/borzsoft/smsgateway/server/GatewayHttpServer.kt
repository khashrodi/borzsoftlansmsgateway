package com.borzsoft.smsgateway.server

import android.content.Context
import com.borzsoft.smsgateway.db.AppDatabase
import com.borzsoft.smsgateway.db.entity.Session
import com.borzsoft.smsgateway.security.TokenManager
import com.borzsoft.smsgateway.service.SmsEngine
import com.borzsoft.smsgateway.utils.DateUtils
import com.borzsoft.smsgateway.utils.NetworkUtils
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

class GatewayHttpServer(
    private val context: Context,
    private val port: Int = 8080
) : NanoHTTPD(port) {

    private val gson = Gson()
    private val db = AppDatabase.create(context)
    private val smsEngine by lazy { SmsEngine(context, db.smsLogDao()) }
    private val rateLimiter = ConcurrentHashMap<String, ArrayDeque<Long>>()

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri.trimEnd('/')
        val method = session.method
        val ip = extractIp(session)

        // Add CORS
        if (method == Method.OPTIONS) return corsOk()

        // LAN only
        if (!NetworkUtils.isLanIp(ip)) return err(403, "Access restricted to LAN only")

        // Serve static web assets
        if (uri.isEmpty() || uri == "/index.html") {
            return serveAsset("web/index.html", "text/html; charset=utf-8")
        }
        if (uri.startsWith("/static")) {
            val path = "web$uri"
            val mime = when {
                uri.endsWith(".css") -> "text/css"
                uri.endsWith(".js") -> "application/javascript"
                uri.endsWith(".svg") -> "image/svg+xml"
                uri.endsWith(".png") -> "image/png"
                uri.endsWith(".ico") -> "image/x-icon"
                else -> "text/plain"
            }
            return serveAsset(path, mime)
        }

        return when {
            uri == "/api/status" && method == Method.GET -> handleStatus()
            uri == "/api/send" && method == Method.POST -> handleSend(session, ip)
            uri == "/api/logs" && method == Method.GET -> handleLogs(session)
            uri == "/api/session" && method == Method.POST -> handleCreateSession(session, ip)
            uri == "/api/session" && method == Method.DELETE -> handleRevokeSession(session)
            uri == "/api/sessions" && method == Method.GET -> handleListSessions()
            uri == "/web/send" && method == Method.POST -> handleWebSend(session, ip)
            uri == "/api/healthz" && method == Method.GET -> ok(mapOf("ok" to true))
            else -> err(404, "Not found: $uri")
        }
    }

    // ─── Handlers ───

    private fun handleStatus(): Response {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val sentToday = runBlocking { db.smsLogDao().countByDate(today) }
        val activeSessions = runBlocking { db.sessionDao().getActiveSessions().size }
        return ok(mapOf(
            "app" to "BorzSoft LAN SMS Gateway",
            "version" to "1.0.0",
            "status" to "online",
            "serverIp" to NetworkUtils.getLocalIp(),
            "port" to port,
            "smsSentToday" to sentToday,
            "activeSessions" to activeSessions,
            "timestamp" to DateUtils.now()
        ))
    }

    private fun handleSend(session: IHTTPSession, ip: String): Response {
        val body = readBody(session)
        data class SendReq(val phone: String?, val message: String?, val sim: String?)
        val req = try { gson.fromJson(body, SendReq::class.java) } catch (e: Exception) { return err(400, "Invalid JSON") }
        if (req.phone.isNullOrBlank()) return err(400, "phone is required")
        if (req.message.isNullOrBlank()) return err(400, "message is required")
        val result = runBlocking { smsEngine.send(req.phone, req.message, req.sim ?: "default", ip, "API") }
        return if (result.success) ok(mapOf("success" to true, "logId" to result.logId, "message" to "SMS queued"))
        else err(500, result.error ?: "Send failed")
    }

    private fun handleLogs(session: IHTTPSession): Response {
        val limit = session.parms["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 100
        val logs = runBlocking { db.smsLogDao().getRecent(limit) }
        return ok(mapOf("logs" to logs, "count" to logs.size))
    }

    private fun handleCreateSession(session: IHTTPSession, ip: String): Response {
        val token = TokenManager.generate()
        val expiresAt = TokenManager.expiryStr(10)
        val webSession = Session(
            token = token, clientIp = ip,
            createdAt = DateUtils.now(), expiresAt = expiresAt
        )
        runBlocking { db.sessionDao().insert(webSession) }
        val gatewayIp = NetworkUtils.getLocalIp()
        return ok(mapOf(
            "token" to token,
            "expiresAt" to expiresAt,
            "webUrl" to "http://$gatewayIp:$port",
            "qrPayload" to mapOf(
                "token" to token,
                "ip" to gatewayIp,
                "port" to port,
                "expires" to expiresAt
            )
        ))
    }

    private fun handleRevokeSession(session: IHTTPSession): Response {
        val body = readBody(session)
        @Suppress("UNCHECKED_CAST")
        val map = try { gson.fromJson(body, Map::class.java) as? Map<String, Any> } catch (e: Exception) { null }
        val token = map?.get("token") as? String
        runBlocking {
            if (token != null) db.sessionDao().revoke(token)
            else db.sessionDao().revokeAll()
        }
        return ok(mapOf("success" to true, "message" to if (token != null) "Session revoked" else "All sessions revoked"))
    }

    private fun handleListSessions(): Response {
        val sessions = runBlocking { db.sessionDao().getActiveSessions() }
        return ok(mapOf("sessions" to sessions, "count" to sessions.size))
    }

    private fun handleWebSend(session: IHTTPSession, ip: String): Response {
        val body = readBody(session)
        data class WebSendReq(val token: String?, val phone: String?, val message: String?, val sim: String?)
        val req = try { gson.fromJson(body, WebSendReq::class.java) } catch (e: Exception) { return err(400, "Invalid JSON") }

        if (req.token.isNullOrBlank()) return err(401, "token is required")

        val webSession = runBlocking { db.sessionDao().getActive(req.token) }
            ?: return err(401, "Invalid or expired token")

        if (TokenManager.isExpired(webSession.expiresAt)) {
            runBlocking { db.sessionDao().revoke(req.token) }
            return err(401, "Token expired — please generate a new QR code")
        }

        if (webSession.clientIp != "*" && webSession.clientIp != ip) {
            return err(403, "Session IP mismatch — token bound to ${webSession.clientIp}")
        }

        if (!checkRate(req.token)) return err(429, "Rate limit: max 10 SMS/minute per session")
        if (req.phone.isNullOrBlank()) return err(400, "phone is required")
        if (req.message.isNullOrBlank()) return err(400, "message is required")

        val result = runBlocking { smsEngine.send(req.phone, req.message, req.sim ?: "default", ip, "WEB") }
        if (result.success) {
            runBlocking { db.sessionDao().incrementSent(req.token, DateUtils.now()) }
        }
        return if (result.success) ok(mapOf("success" to true, "logId" to result.logId))
        else err(500, result.error ?: "Send failed")
    }

    // ─── Helpers ───

    private fun checkRate(token: String): Boolean {
        val now = System.currentTimeMillis()
        val q = rateLimiter.getOrPut(token) { ArrayDeque() }
        q.removeAll { now - it > 60_000L }
        if (q.size >= 10) return false
        q.addLast(now)
        return true
    }

    private fun extractIp(session: IHTTPSession): String =
        session.headers["x-forwarded-for"]?.split(",")?.firstOrNull()?.trim()
            ?: session.headers["http-client-ip"]
            ?: session.remoteIpAddress
            ?: "unknown"

    private fun readBody(session: IHTTPSession): String {
        val len = session.headers["content-length"]?.toIntOrNull() ?: return ""
        if (len <= 0) return ""
        val buf = ByteArray(len)
        session.inputStream.read(buf, 0, len)
        return String(buf, Charsets.UTF_8)
    }

    private fun serveAsset(path: String, mime: String): Response {
        return try {
            val stream: InputStream = context.assets.open(path)
            newChunkedResponse(Response.Status.OK, mime, stream)
                .also { addCors(it) }
        } catch (e: Exception) {
            err(404, "Asset not found: $path")
        }
    }

    private fun ok(data: Any): Response {
        val r = newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(data))
        addCors(r)
        return r
    }

    private fun err(code: Int, msg: String): Response {
        val status = when (code) {
            400 -> Response.Status.BAD_REQUEST
            401 -> Response.Status.UNAUTHORIZED
            403 -> Response.Status.FORBIDDEN
            404 -> Response.Status.NOT_FOUND
            else -> Response.Status.INTERNAL_ERROR
        }
        val r = newFixedLengthResponse(status, "application/json",
            gson.toJson(mapOf("error" to msg, "code" to code)))
        addCors(r)
        return r
    }

    private fun corsOk(): Response {
        val r = newFixedLengthResponse(Response.Status.OK, "text/plain", "")
        addCors(r)
        return r
    }

    private fun addCors(r: Response) {
        r.addHeader("Access-Control-Allow-Origin", "*")
        r.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
        r.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Token")
        r.addHeader("X-Powered-By", "BorzSoft SMS Gateway v1.0.0")
    }
}
