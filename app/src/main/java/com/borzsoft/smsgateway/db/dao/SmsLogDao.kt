package com.borzsoft.smsgateway.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.borzsoft.smsgateway.db.entity.SmsLog

@Dao
interface SmsLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SmsLog): Long

    @Query("SELECT * FROM sms_logs ORDER BY id DESC LIMIT 500")
    fun observeAll(): LiveData<List<SmsLog>>

    @Query("SELECT * FROM sms_logs ORDER BY id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<SmsLog>

    @Query("SELECT * FROM sms_logs WHERE id = :id")
    suspend fun getById(id: Long): SmsLog?

    @Query("SELECT COUNT(*) FROM sms_logs WHERE sentAt LIKE :datePrefix || '%'")
    suspend fun countByDate(datePrefix: String): Int

    @Query("SELECT COUNT(*) FROM sms_logs WHERE status = 'SENT' OR status = 'DELIVERED'")
    fun observeSentCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM sms_logs WHERE status = 'FAILED'")
    fun observeFailedCount(): LiveData<Int>

    @Query("UPDATE sms_logs SET status = :status, updatedAt = :updatedAt, errorMsg = :error WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: String, error: String? = null)

    @Query("DELETE FROM sms_logs WHERE id IN (SELECT id FROM sms_logs ORDER BY id ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("SELECT COUNT(*) FROM sms_logs")
    suspend fun count(): Int

    @Query("DELETE FROM sms_logs")
    suspend fun clearAll()
}
