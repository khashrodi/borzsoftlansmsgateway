package com.borzsoft.smsgateway.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs", indices = [Index("sentAt"), Index("status")])
data class SmsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phone: String,
    val message: String,
    val sim: String = "default",
    val status: String = "PENDING",   // PENDING | SENT | FAILED | DELIVERED
    val errorMsg: String? = null,
    val senderIp: String = "app",
    val source: String = "APP",       // APP | API | WEB
    val sentAt: String,
    val updatedAt: String = sentAt,
    val charCount: Int = message.length,
    val partsCount: Int = 1
)
