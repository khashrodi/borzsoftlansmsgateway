package com.borzsoft.smsgateway.db.dao

import androidx.room.*
import com.borzsoft.smsgateway.db.entity.IpLog

@Dao
interface IpLogDao {

    @Insert
    suspend fun insert(log: IpLog)

    @Query("SELECT * FROM ip_logs ORDER BY id DESC LIMIT 200")
    suspend fun getAll(): List<IpLog>

    @Query("SELECT COUNT(*) FROM ip_logs WHERE ip = :ip AND timestamp > :since")
    suspend fun countSince(ip: String, since: String): Int

    @Query("DELETE FROM ip_logs WHERE id IN (SELECT id FROM ip_logs ORDER BY id ASC LIMIT :n)")
    suspend fun deleteOldest(n: Int)
}
