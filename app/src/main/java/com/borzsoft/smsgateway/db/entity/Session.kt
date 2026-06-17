package com.borzsoft.smsgateway.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions", indices = [Index("isRevoked"), Index("expiresAt")])
data class Session(
    @PrimaryKey val token: String,
    val clientIp: String,
    val label: String = "Browser",
    val createdAt: String,
    val expiresAt: String,
    val lastUsedAt: String = createdAt,
    val messagesSent: Int = 0,
    val isRevoked: Boolean = false
)
