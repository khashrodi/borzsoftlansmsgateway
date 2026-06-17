package com.borzsoft.smsgateway.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phone: String,
    val message: String,
    val sim: String,
    val status: String,  // SENT, FAILED, PENDING
    val senderIp: String,
    val sentAt: String,
    val source: String   // API, WEB, APP
)
