package com.borzsoft.smsgateway.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "web_sessions")
data class WebSession(
    @PrimaryKey val token: String,
    val clientIp: String,
    val createdAt: String,
    val expiresAt: String,
    val messagesSent: Int = 0,
    val isRevoked: Boolean = false
)
