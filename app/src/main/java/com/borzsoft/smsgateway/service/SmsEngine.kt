package com.borzsoft.smsgateway.service

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import com.borzsoft.smsgateway.db.AppDatabase
import com.borzsoft.smsgateway.db.entity.SmsLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SmsEngine(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    data class SendResult(val success: Boolean, val logId: Long, val error: String? = null)

    fun sendSms(phone: String, message: String, sim: String, senderIp: String): SendResult {
        return try {
            val smsManager = getSmsManager(sim)
            val parts = smsManager.divideMessage(message)

            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            var logId = 0L

            scope.launch {
                val log = SmsLog(
                    phone = phone,
                    message = message,
                    sim = sim,
                    status = "PENDING",
                    senderIp = senderIp,
                    sentAt = now,
                    source = if (senderIp == "app") "APP" else "API"
                )
                logId = db.smsLogDao().insert(log)
            }

            val sentAction = "SMS_SENT_$phone"
            val deliveredAction = "SMS_DELIVERED_$phone"

            val sentIntent = PendingIntent.getBroadcast(
                context, phone.hashCode(),
                Intent(sentAction),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val deliveredIntent = PendingIntent.getBroadcast(
                context, phone.hashCode() + 1,
                Intent(deliveredAction),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (parts.size > 1) {
                val sentList = ArrayList<PendingIntent>().apply { repeat(parts.size) { add(sentIntent) } }
                val deliveredList = ArrayList<PendingIntent>().apply { repeat(parts.size) { add(deliveredIntent) } }
                smsManager.sendMultipartTextMessage(phone, null, parts, sentList, deliveredList)
            } else {
                smsManager.sendTextMessage(phone, null, message, sentIntent, deliveredIntent)
            }

            SendResult(success = true, logId = logId)
        } catch (e: Exception) {
            SendResult(success = false, logId = 0, error = e.message)
        }
    }

    private fun getSmsManager(sim: String): SmsManager {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subscriptions = subscriptionManager.activeSubscriptionInfoList

            if (subscriptions != null && subscriptions.size >= 2) {
                return when (sim.lowercase()) {
                    "sim1" -> context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptions[0].subscriptionId)
                    "sim2" -> context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptions[1].subscriptionId)
                    else -> context.getSystemService(SmsManager::class.java)
                }
            }
        }
        @Suppress("DEPRECATION")
        return SmsManager.getDefault()
    }
}
