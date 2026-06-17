package com.borzsoft.smsgateway.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.borzsoft.smsgateway.db.entity.WebSession

@Dao
interface WebSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WebSession)

    @Query("SELECT * FROM web_sessions WHERE token = :token AND isRevoked = 0")
    suspend fun getSession(token: String): WebSession?

    @Query("SELECT * FROM web_sessions WHERE isRevoked = 0 ORDER BY createdAt DESC")
    fun getActiveSessions(): LiveData<List<WebSession>>

    @Query("UPDATE web_sessions SET isRevoked = 1 WHERE token = :token")
    suspend fun revokeSession(token: String)

    @Query("UPDATE web_sessions SET isRevoked = 1")
    suspend fun revokeAllSessions()

    @Query("UPDATE web_sessions SET messagesSent = messagesSent + 1 WHERE token = :token")
    suspend fun incrementMessageCount(token: String)
}
