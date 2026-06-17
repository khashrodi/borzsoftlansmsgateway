package com.borzsoft.smsgateway.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.borzsoft.smsgateway.db.AppDatabase
import com.borzsoft.smsgateway.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val logId = intent.getLongExtra("logId", -1L)
        if (logId < 0) return

        val dao = AppDatabase.create(context).smsLogDao()
        val scope = CoroutineScope(Dispatchers.IO)
        val now = DateUtils.now()

        when (intent.action) {
            "com.borzsoft.SMS_SENT" -> {
                val status = when (resultCode) {
                    Activity.RESULT_OK -> "SENT"
                    SmsManager.RESULT_ERROR_NO_SERVICE -> "FAILED"
                    SmsManager.RESULT_ERROR_RADIO_OFF -> "FAILED"
                    SmsManager.RESULT_ERROR_NULL_PDU -> "FAILED"
                    else -> "FAILED"
                }
                val error = when (resultCode) {
                    Activity.RESULT_OK -> null
                    SmsManager.RESULT_ERROR_NO_SERVICE -> "No service"
                    SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio off"
                    SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU"
                    else -> "Generic failure (code $resultCode)"
                }
                scope.launch { dao.updateStatus(logId, status, now, error) }
            }
            "com.borzsoft.SMS_DELIVERED" -> {
                if (resultCode == Activity.RESULT_OK) {
                    scope.launch { dao.updateStatus(logId, "DELIVERED", now) }
                }
            }
        }
    }
}
