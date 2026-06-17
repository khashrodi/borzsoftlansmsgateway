package com.borzsoft.smsgateway.server

import android.content.Context
import com.borzsoft.smsgateway.db.AppDatabase
import com.borzsoft.smsgateway.db.entity.WebSession
import com.borzsoft.smsgateway.security.TokenManager
import com.borzsoft.smsgateway.service.SmsEngine
import com.borzsoft.smsgateway.utils.NetworkUtils
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

class GatewayHttpServer(
    private val context: Context,
    port: Int = 8080
) : NanoHTTPD(port) {

    private val gson = Gson()
    private val db = AppDatabase.getInstance(context)
    private val smsEngine = SmsEngine(context)

    // In-memory rate limiter: token -> list of timestamps
    private val rateLimiter = ConcurrentHashMap<String, ArrayDeque<Long>>()

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        val clientIp = session.headers["http-client-ip"]
            ?: session.headers["x-forwarded-for"]
            ?: session.remoteIpAddress
            ?: "unknown"

        // LAN-only restriction
        if (!NetworkUtils.isLanIp(clientIp)) {
            return jsonError(403, "Access denied: LAN only")
        }

        // Serve web dashboard HTML
        if (uri == "/" || uri == "/index.html") {
            val html = context.assets.open("web/index.html").bufferedReader().readText()
            return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
        }

        // Serve static assets
        if (uri.startsWith("/static/")) {
            return try {
                val path = "web" + uri
                val mime = when {
                    uri.endsWith(".css") -> "text/css"
                    uri.endsWith(".js") -> "application/javascript"
                    uri.endsWith(".png") -> "image/png"
                    uri.endsWith(".svg") -> "image/svg+xml"
                    else -> "text/plain"
                }
                val content = context.assets.open(path).bufferedReader().readText()
                newFixedLengthResponse(Response.Status.OK, mime, content)
            } catch (e: Exception) {
                jsonError(404, "Asset not found")
            }
        }

        return when {
            uri == "/api/status" && method == Method.GET -> handleStatus(clientIp)
            uri == "/api/send" && method == Method.POST -> handleApiSend(session, clientIp)
            uri == "/api/logs" && method == Method.GET -> handleLogs(session, clientIp)
            uri == "/api/web/session" && method == Method.POST -> handleCreateSession(clientIp)
            uri == "/api/web/session" && method == Method.DELETE -> handleRevokeSession(session, clientIp)
            uri == "/web/send" && method == Method.POST -> handleWebSend(session, clientIp)
            else -> jsonError(404, "Endpoint not found")
        }
    }

    private fun handleStatus(clientIp: String): Response {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val sentToday = runBlocking { db.smsLogDao().countToday(today) }
        val ip = NetworkUtils.getLocalIpAddress(context)

        val status = mapOf(
            "app" to "BorzSoft LAN SMS Gateway",
            "version" to "1.0.0",
            "status" to "online",
            "serverIp" to ip,
            "port" to 8080,
            "smsSentToday" to sentToday,
            "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        return jsonOk(status)
    }

    private fun handleApiSend(session: IHTTPSession, clientIp: String): Response {
        val body = readBody(session)
        val req = try { gson.fromJson(body, SendRequest::class.java) } catch (e: Exception) {
            return jsonError(400, "Invalid JSON body")
        }
        if (req.phone.isNullOrBlank() || req.message.isNullOrBlank()) {
            return jsonError(400, "phone and message are required")
        }
        val result = smsEngine.sendSms(req.phone, req.message, req.sim ?: "default", clientIp)
        return if (result.success) {
            jsonOk(mapOf("success" to true, "logId" to result.logId, "message" to "SMS queued"))
        } else {
            jsonError(500, result.error ?: "Failed to send SMS")
        }
    }

    private fun handleWebSend(session: IHTTPSession, clientIp: String): Response {
        val body = readBody(session)
        val req = try { gson.fromJson(body, WebSendRequest::class.java) } catch (e: Exception) {
            return jsonError(400, "Invalid JSON body")
        }
        if (req.token.isNullOrBlank()) return jsonError(401, "Token required")

        val webSession = runBlocking { db.webSessionDao().getSession(req.token) }
            ?: return jsonError(401, "Invalid or expired token")

        if (TokenManager.isTokenExpired(webSession.expiresAt)) {
            return jsonError(401, "Token expired")
        }

        // IP binding
        if (webSession.clientIp != clientIp) {
            return jsonError(403, "Session bound to different IP")
        }

        // Rate limit: 10/minute per token
        if (!checkRateLimit(req.token)) {
            return jsonError(429, "Rate limit exceeded: max 10 messages per minute")
        }

        if (req.phone.isNullOrBlank() || req.message.isNullOrBlank()) {
            return jsonError(400, "phone and message are required")
        }

        val result = smsEngine.sendSms(req.phone, req.message, req.sim ?: "default", clientIp)

        if (result.success) {
            runBlocking { db.webSessionDao().incrementMessageCount(req.token) }
        }

        return if (result.success) {
            jsonOk(mapOf("success" to true, "logId" to result.logId))
        } else {
            jsonError(500, result.error ?: "Failed to send SMS")
        }
    }

    private fun handleLogs(session: IHTTPSession, clientIp: String): Response {
        val logs = runBlocking { db.smsLogDao().getRecentLogs(50) }
        return jsonOk(mapOf("logs" to logs, "count" to logs.size))
    }

    private fun handleCreateSession(clientIp: String): Response {
        val token = TokenManager.generateToken()
        val expiresAt = TokenManager.expiryTime(10)
        val webSession = WebSession(
            token = token,
            clientIp = clientIp,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            expiresAt = expiresAt
        )
        runBlocking { db.webSessionDao().insert(webSession) }
        val ip = NetworkUtils.getLocalIpAddress(context)
        return jsonOk(mapOf(
            "token" to token,
            "expires" to expiresAt,
            "webUrl" to "http://$ip:8080",
            "qrData" to """{"token":"$token","expires":"$expiresAt","ip":"$ip","port":8080}"""
        ))
    }

    private fun handleRevokeSession(session: IHTTPSession, clientIp: String): Response {
        val body = readBody(session)
        val req = try { gson.fromJson(body, mapOf<String, String>()::class.java) } catch (e: Exception) {
            mapOf<String, String>()
        }
        val token = req["token"]
        if (token != null) {
            runBlocking { db.webSessionDao().revokeSession(token) }
        } else {
            runBlocking { db.webSessionDao().revokeAllSessions() }
        }
        return jsonOk(mapOf("success" to true, "message" to "Session(s) revoked"))
    }

    private fun checkRateLimit(token: String): Boolean {
        val now = System.currentTimeMillis()
        val window = 60_000L // 1 minute
        val timestamps = rateLimiter.getOrPut(token) { ArrayDeque() }
        // Remove old entries
        while (timestamps.isNotEmpty() && now - timestamps.first() > window) {
            timestamps.removeFirst()
        }
        if (timestamps.size >= 10) return false
        timestamps.addLast(now)
        return true
    }

    private fun readBody(session: IHTTPSession): String {
        val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
        val buffer = ByteArray(contentLength)
        session.inputStream.read(buffer, 0, contentLength)
        return String(buffer)
    }

    private fun jsonOk(data: Any): Response {
        val json = gson.toJson(data)
        val resp = newFixedLengthResponse(Response.Status.OK, "application/json", json)
        addCorsHeaders(resp)
        return resp
    }

    private fun jsonError(code: Int, message: String): Response {
        val json = gson.toJson(mapOf("error" to message, "code" to code))
        val status = when (code) {
            400 -> Response.Status.BAD_REQUEST
            401 -> Response.Status.UNAUTHORIZED
            403 -> Response.Status.FORBIDDEN
            404 -> Response.Status.NOT_FOUND
            429 -> Response.Status.lookup(429)
            else -> Response.Status.INTERNAL_ERROR
        }
        val resp = newFixedLengthResponse(status, "application/json", json)
        addCorsHeaders(resp)
        return resp
    }

    private fun addCorsHeaders(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
        response.addHeader("X-Powered-By", "BorzSoft SMS Gateway")
    }

    data class SendRequest(val phone: String?, val message: String?, val sim: String?)
    data class WebSendRequest(val token: String?, val phone: String?, val message: String?, val sim: String?)
}
