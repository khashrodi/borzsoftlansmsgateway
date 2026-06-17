package com.borzsoft.smsgateway.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateUtils {
    private val fullFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun now(): String = LocalDateTime.now().format(fullFmt)
    fun time(iso: String): String = try {
        LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME).format(timeFmt)
    } catch (e: Exception) { iso.take(19).replace("T", " ") }

    fun today(): String = LocalDate.now().format(dateFmt)
}
