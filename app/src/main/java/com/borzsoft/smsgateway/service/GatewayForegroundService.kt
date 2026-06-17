package com.borzsoft.smsgateway.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.borzsoft.smsgateway.R
import com.borzsoft.smsgateway.server.GatewayHttpServer
import com.borzsoft.smsgateway.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GatewayForegroundService : Service() {

    private var httpServer: GatewayHttpServer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "borzsoft_gateway_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.borzsoft.smsgateway.STOP"
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        startServer()
        isRunning = true

        return START_STICKY
    }

    private fun startServer() {
        scope.launch {
            try {
                httpServer?.stop()
                httpServer = GatewayHttpServer(applicationContext, 8080)
                httpServer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun buildNotification(): Notification {
        val ip = NetworkUtils.getLocalIpAddress(applicationContext)
        val stopIntent = Intent(this, GatewayForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BorzSoft LAN SMS Gateway")
            .setContentText("Running at http://$ip:8080")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop", stopPending)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BorzSoft Gateway Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "BorzSoft LAN SMS Gateway foreground service"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        httpServer?.stop()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
