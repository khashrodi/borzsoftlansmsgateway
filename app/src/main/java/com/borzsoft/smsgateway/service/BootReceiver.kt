package com.borzsoft.smsgateway.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val autoStart = prefs.getBoolean("auto_start_on_boot", true)
        if (autoStart) {
            val svc = Intent(context, GatewayService::class.java)
            ContextCompat.startForegroundService(context, svc)
        }
    }
}
