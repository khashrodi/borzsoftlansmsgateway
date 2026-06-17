package com.borzsoft.smsgateway.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    fun getLocalIp(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()?.asSequence()
                ?.filter { !it.isLoopback && it.isUp }
                ?.flatMap { it.inetAddresses.asSequence() }
                ?.filterIsInstance<Inet4Address>()
                ?.map { it.hostAddress ?: "" }
                ?.firstOrNull { it.isNotEmpty() } ?: "127.0.0.1"
        } catch (e: Exception) { "127.0.0.1" }
    }

    fun isLanIp(ip: String): Boolean =
        ip.startsWith("192.168.") ||
        ip.startsWith("10.") ||
        ip.startsWith("172.16.") || ip.startsWith("172.17.") ||
        ip.startsWith("172.18.") || ip.startsWith("172.19.") ||
        ip.startsWith("172.2") || ip.startsWith("172.30.") ||
        ip.startsWith("172.31.") ||
        ip == "127.0.0.1" || ip == "::1" || ip == "0:0:0:0:0:0:0:1"

    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork?.let {
            cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: false
    }
}
