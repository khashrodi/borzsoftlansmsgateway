package com.borzsoft.smsgateway.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.borzsoft.smsgateway.BorzSoftApp
import com.borzsoft.smsgateway.R
import com.borzsoft.smsgateway.db.AppDatabase
import com.borzsoft.smsgateway.server.GatewayHttpServer
import com.borzsoft.smsgateway.ui.activities.MainActivity
import com.borzsoft.smsgateway.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GatewayService : LifecycleService() {

    @Inject lateinit var db: AppDatabase

    private var server: GatewayHttpServer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_STOP = "com.borzsoft.ACTION_STOP"
        const val PORT = 8080
        var isRunning = false
            private set
        var connectedCount = 0
        var smsSentCount = 0
    }

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }

        startForeground(1, buildNotification())
        startServer()
        isRunning = true
        updateSmsSentCount()

        return START_STICKY
    }

    private fun startServer() {
        try {
            server?.stop()
            server = GatewayHttpServer(applicationContext, PORT)
            server?.start(10_000, false)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateSmsSentCount() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val count = db.smsLogDao().countByDate(
                    java.time.LocalDate.now().toString()
                )
                smsSentCount = count
                updateNotification()
            } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun updateNotification() {
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm.notify(1, buildNotification())
    }

    private fun buildNotification(): Notification {
        val ip = NetworkUtils.getLocalIp()

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, GatewayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, BorzSoftApp.CHANNEL_GATEWAY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("BorzSoft LAN SMS Gateway")
            .setContentText("http://$ip:$PORT  •  SMS Today: $smsSentCount")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Running at http://$ip:$PORT\nSMS Sent Today: $smsSentCount\nSessions Active: $connectedCount")
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BorzSoft:GatewayWakeLock"
        ).apply { acquire(24 * 60 * 60 * 1000L) }
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        wakeLock?.release()
        isRunning = false
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)
}
