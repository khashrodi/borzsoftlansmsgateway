package com.borzsoft.smsgateway.security

import android.content.Context
import java.net.NetworkInterface

object DeviceLock {

    private const val AUTHORIZED_MAC = "fc:d9:08:c3:20:60"

    fun isAuthorized(context: Context): Boolean {
        val mac = getWifiMac()
        return mac != null && mac.equals(AUTHORIZED_MAC, ignoreCase = true)
    }

    private fun getWifiMac(): String? {
        return try {
            val ifaces = NetworkInterface.getNetworkInterfaces() ?: return null
            ifaces.asSequence()
                .filter { it.name.equals("wlan0", ignoreCase = true) }
                .mapNotNull { iface ->
                    iface.hardwareAddress?.let { bytes ->
                        bytes.joinToString(":") { "%02x".format(it) }
                    }
                }
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
