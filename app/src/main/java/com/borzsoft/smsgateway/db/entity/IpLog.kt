package com.borzsoft.smsgateway.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ip_logs")
data class IpLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ip: String,
    val action: String,
    val detail: String = "",
    val success: Boolean = true,
    val timestamp: String
)
