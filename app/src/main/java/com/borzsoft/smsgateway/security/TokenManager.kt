package com.borzsoft.smsgateway.security

import android.content.Context
import com.borzsoft.smsgateway.db.AppDatabase
import com.borzsoft.smsgateway.db.entity.WebSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

object TokenManager {
    private val random = SecureRandom()

    fun generateToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun isTokenExpired(expiresAt: String): Boolean {
        return try {
            val expires = LocalDateTime.parse(expiresAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            LocalDateTime.now().isAfter(expires)
        } catch (e: Exception) {
            true
        }
    }

    fun expiryTime(minutes: Long = 10): String {
        return LocalDateTime.now().plusMinutes(minutes)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}
