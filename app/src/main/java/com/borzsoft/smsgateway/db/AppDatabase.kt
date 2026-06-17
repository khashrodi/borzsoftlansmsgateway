package com.borzsoft.smsgateway.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.borzsoft.smsgateway.db.dao.IpLogDao
import com.borzsoft.smsgateway.db.dao.SessionDao
import com.borzsoft.smsgateway.db.dao.SmsLogDao
import com.borzsoft.smsgateway.db.entity.IpLog
import com.borzsoft.smsgateway.db.entity.Session
import com.borzsoft.smsgateway.db.entity.SmsLog

@Database(
    entities = [SmsLog::class, Session::class, IpLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun smsLogDao(): SmsLogDao
    abstract fun sessionDao(): SessionDao
    abstract fun ipLogDao(): IpLogDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun create(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "borzsoft_smsgateway.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
