package com.borzsoft.smsgateway.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.borzsoft.smsgateway.db.entity.Session

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: Session)

    @Query("SELECT * FROM sessions WHERE token = :token AND isRevoked = 0 LIMIT 1")
    suspend fun getActive(token: String): Session?

    @Query("SELECT * FROM sessions WHERE isRevoked = 0 ORDER BY createdAt DESC")
    fun observeActive(): LiveData<List<Session>>

    @Query("SELECT * FROM sessions WHERE isRevoked = 0 ORDER BY createdAt DESC")
    suspend fun getActiveSessions(): List<Session>

    @Query("SELECT COUNT(*) FROM sessions WHERE isRevoked = 0")
    fun observeActiveCount(): LiveData<Int>

    @Query("UPDATE sessions SET isRevoked = 1 WHERE token = :token")
    suspend fun revoke(token: String)

    @Query("UPDATE sessions SET isRevoked = 1")
    suspend fun revokeAll()

    @Query("UPDATE sessions SET messagesSent = messagesSent + 1, lastUsedAt = :now WHERE token = :token")
    suspend fun incrementSent(token: String, now: String)

    @Query("DELETE FROM sessions WHERE isRevoked = 1 AND expiresAt < :cutoff")
    suspend fun cleanExpired(cutoff: String)
}
