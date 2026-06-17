package com.borzsoft.smsgateway.security

import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

object TokenManager {
    private val rng = SecureRandom()
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun generate(): String {
        val bytes = ByteArray(32)
        rng.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun isExpired(expiresAt: String): Boolean = try {
        LocalDateTime.parse(expiresAt, fmt).isBefore(LocalDateTime.now())
    } catch (e: Exception) { true }

    fun expiryStr(minutes: Long = 10): String =
        LocalDateTime.now().plusMinutes(minutes).format(fmt)

    fun nowStr(): String = LocalDateTime.now().format(fmt)
    fun todayStr(): String = LocalDateTime.now().toLocalDate().toString()
}
