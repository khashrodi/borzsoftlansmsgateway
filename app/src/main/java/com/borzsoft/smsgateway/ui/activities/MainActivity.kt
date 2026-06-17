package com.borzsoft.smsgateway.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.borzsoft.smsgateway.R
import com.borzsoft.smsgateway.db.AppDatabase
import com.borzsoft.smsgateway.db.entity.WebSession
import com.borzsoft.smsgateway.security.TokenManager
import com.borzsoft.smsgateway.service.GatewayForegroundService
import com.borzsoft.smsgateway.utils.NetworkUtils
import com.borzsoft.smsgateway.utils.QrUtils
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.getInstance(this) }
    private lateinit var tvStatus: TextView
    private lateinit var tvIp: TextView
    private lateinit var tvSmsCount: TextView
    private lateinit var btnToggle: Button
    private lateinit var ivQr: ImageView
    private lateinit var tvToken: TextView
    private lateinit var btnRefreshToken: Button
    private lateinit var btnRevokeAll: Button
    private lateinit var rvSessions: ListView
    private lateinit var lvLogs: ListView

    companion object {
        private val REQUIRED_PERMISSIONS = buildList {
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        private const val PERM_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_status)
        tvIp = findViewById(R.id.tv_ip)
        tvSmsCount = findViewById(R.id.tv_sms_count)
        btnToggle = findViewById(R.id.btn_toggle)
        ivQr = findViewById(R.id.iv_qr)
        tvToken = findViewById(R.id.tv_token)
        btnRefreshToken = findViewById(R.id.btn_refresh_token)
        btnRevokeAll = findViewById(R.id.btn_revoke_all)
        lvLogs = findViewById(R.id.lv_logs)

        checkPermissions()
        setupUI()
        observeData()
    }

    private fun setupUI() {
        val ip = NetworkUtils.getLocalIpAddress(this)
        tvIp.text = "http://$ip:8080"

        btnToggle.setOnClickListener {
            if (GatewayForegroundService.isRunning) {
                stopService(Intent(this, GatewayForegroundService::class.java))
                updateServerStatus(false)
            } else {
                ContextCompat.startForegroundService(this,
                    Intent(this, GatewayForegroundService::class.java))
                updateServerStatus(true)
            }
        }

        btnRefreshToken.setOnClickListener { generateNewSession() }

        btnRevokeAll.setOnClickListener {
            lifecycleScope.launch {
                db.webSessionDao().revokeAllSessions()
                Toast.makeText(this@MainActivity, "All sessions revoked", Toast.LENGTH_SHORT).show()
                ivQr.setImageDrawable(null)
                tvToken.text = "Tap 'Generate QR' to create a new session"
            }
        }

        updateServerStatus(GatewayForegroundService.isRunning)
        generateNewSession()
    }

    private fun generateNewSession() {
        lifecycleScope.launch {
            val token = TokenManager.generateToken()
            val ip = NetworkUtils.getLocalIpAddress(this@MainActivity)
            val expiresAt = TokenManager.expiryTime(10)

            val session = WebSession(
                token = token,
                clientIp = "*",
                createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                expiresAt = expiresAt
            )
            db.webSessionDao().insert(session)

            val qrData = """{"token":"$token","expires":"$expiresAt","ip":"$ip","port":8080}"""
            val qrBitmap = QrUtils.generateQr(qrData, 400)
            ivQr.setImageBitmap(qrBitmap)
            tvToken.text = token
        }
    }

    private fun observeData() {
        db.smsLogDao().getAllLogs().observe(this) { logs ->
            tvSmsCount.text = "SMS Today: ${logs.count { it.sentAt.startsWith(
                java.time.LocalDate.now().toString())}}"
            val adapter = ArrayAdapter(this,
                android.R.layout.simple_list_item_1,
                logs.take(20).map { "[${it.status}] ${it.phone} – ${it.message.take(30)}" })
            lvLogs.adapter = adapter
        }
    }

    private fun updateServerStatus(running: Boolean) {
        tvStatus.text = if (running) "ONLINE" else "OFFLINE"
        tvStatus.setTextColor(ContextCompat.getColor(this,
            if (running) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        btnToggle.text = if (running) "Stop Gateway" else "Start Gateway"
    }

    private fun checkPermissions() {
        val missing = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERM_REQUEST)
        }
    }
}
