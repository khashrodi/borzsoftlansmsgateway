package com.borzsoft.smsgateway

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BorzSoftApp : Application() {

    companion object {
        const val CHANNEL_GATEWAY = "borzsoft_gateway"
        const val CHANNEL_SMS = "borzsoft_sms"
        const val CHANNEL_ALERT = "borzsoft_alert"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_GATEWAY, "Gateway Service", NotificationManager.IMPORTANCE_LOW).apply {
                description = "BorzSoft gateway running notification"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SMS, "SMS Notifications", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Sent/received SMS notifications"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, "Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Important alerts"
            }
        )
    }
}
