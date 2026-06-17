package com.borzsoft.smsgateway.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import com.borzsoft.smsgateway.db.dao.SmsLogDao
import com.borzsoft.smsgateway.db.entity.SmsLog
import com.borzsoft.smsgateway.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsEngine(private val context: Context, private val smsLogDao: SmsLogDao) {

    data class Result(val success: Boolean, val logId: Long, val error: String? = null)

    suspend fun send(phone: String, message: String, sim: String, ip: String, source: String = "API"): Result =
        withContext(Dispatchers.IO) {
            val now = DateUtils.now()
            val log = SmsLog(
                phone = phone, message = message, sim = sim,
                status = "PENDING", senderIp = ip, source = source,
                sentAt = now, charCount = message.length,
                partsCount = estimateParts(message)
            )
            val logId = smsLogDao.insert(log)

            return@withContext try {
                val manager = resolveSmsManager(sim)
                val parts = manager.divideMessage(message)

                val sentPI = PendingIntent.getBroadcast(
                    context, logId.toInt(),
                    Intent("com.borzsoft.SMS_SENT").putExtra("logId", logId),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val delivPI = PendingIntent.getBroadcast(
                    context, (logId + 100000).toInt(),
                    Intent("com.borzsoft.SMS_DELIVERED").putExtra("logId", logId),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (parts.size == 1) {
                    manager.sendTextMessage(phone, null, message, sentPI, delivPI)
                } else {
                    val sentList = ArrayList((1..parts.size).map { sentPI })
                    val delivList = ArrayList((1..parts.size).map { delivPI })
                    manager.sendMultipartTextMessage(phone, null, parts, sentList, delivList)
                }
                Result(true, logId)
            } catch (e: Exception) {
                smsLogDao.updateStatus(logId, "FAILED", DateUtils.now(), e.message)
                Result(false, logId, e.message ?: "Unknown error")
            }
        }

    private fun resolveSmsManager(sim: String): SmsManager {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sm = context.getSystemService(SmsManager::class.java)
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subs = try { subscriptionManager.activeSubscriptionInfoList } catch (e: Exception) { null }
            if (!subs.isNullOrEmpty()) {
                return when (sim.lowercase().trim()) {
                    "sim1" -> sm.createForSubscriptionId(subs[0].subscriptionId)
                    "sim2" -> if (subs.size >= 2) sm.createForSubscriptionId(subs[1].subscriptionId) else sm
                    else -> sm
                }
            }
        }
        @Suppress("DEPRECATION")
        return SmsManager.getDefault()
    }

    private fun estimateParts(msg: String): Int {
        return if (msg.length <= 160) 1 else (msg.length + 152) / 153
    }
}
