package com.borzsoft.smsgateway.security

import android.content.Context
import android.net.wifi.WifiManager
import java.net.NetworkInterface

object DeviceLock {
    private const val AUTHORIZED_MAC = "fc:d9:08:c3:20:60"

    fun isAuthorized(context: Context): Boolean {
        return getDeviceMac(context).equals(AUTHORIZED_MAC, ignoreCase = true)
    }

    private fun getDeviceMac(context: Context): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.name.equals("wlan0", ignoreCase = true)) {
                    val mac = iface.hardwareAddress ?: continue
                    return mac.joinToString(":") { "%02x".format(it) }
                }
            }
            // Fallback: WifiManager (older API)
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val info = wm.connectionInfo
            @Suppress("DEPRECATION")
            info.macAddress
        } catch (e: Exception) {
            null
        }
    }
}
