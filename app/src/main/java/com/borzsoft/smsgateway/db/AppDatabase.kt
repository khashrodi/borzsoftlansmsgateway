package com.borzsoft.smsgateway.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.borzsoft.smsgateway.db.dao.SmsLogDao
import com.borzsoft.smsgateway.db.dao.WebSessionDao
import com.borzsoft.smsgateway.db.entity.IpLog
import com.borzsoft.smsgateway.db.entity.SmsLog
import com.borzsoft.smsgateway.db.entity.WebSession

@Database(
    entities = [SmsLog::class, WebSession::class, IpLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsLogDao(): SmsLogDao
    abstract fun webSessionDao(): WebSessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "borzsoft_gateway.db"
                ).build().also { INSTANCE = it }
            }
    }
}
