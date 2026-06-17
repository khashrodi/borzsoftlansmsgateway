package com.borzsoft.smsgateway.di

import android.content.Context
import com.borzsoft.smsgateway.db.AppDatabase
import com.borzsoft.smsgateway.db.dao.IpLogDao
import com.borzsoft.smsgateway.db.dao.SessionDao
import com.borzsoft.smsgateway.db.dao.SmsLogDao
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        AppDatabase.create(ctx)

    @Provides
    @Singleton
    fun provideSmsLogDao(db: AppDatabase): SmsLogDao = db.smsLogDao()

    @Provides
    @Singleton
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

    @Provides
    @Singleton
    fun provideIpLogDao(db: AppDatabase): IpLogDao = db.ipLogDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setPrettyPrinting().create()
}
