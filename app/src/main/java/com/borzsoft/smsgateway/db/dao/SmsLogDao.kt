package com.borzsoft.smsgateway.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.borzsoft.smsgateway.db.entity.SmsLog

@Dao
interface SmsLogDao {
    @Insert
    suspend fun insert(log: SmsLog): Long

    @Query("SELECT * FROM sms_logs ORDER BY id DESC LIMIT 100")
    fun getAllLogs(): LiveData<List<SmsLog>>

    @Query("SELECT * FROM sms_logs ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 50): List<SmsLog>

    @Query("SELECT COUNT(*) FROM sms_logs WHERE sentAt LIKE :date || '%'")
    suspend fun countToday(date: String): Int

    @Query("UPDATE sms_logs SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
