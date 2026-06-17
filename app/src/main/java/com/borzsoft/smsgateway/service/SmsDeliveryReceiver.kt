package com.borzsoft.smsgateway.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager

class SmsDeliveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "SMS_SENT" -> {
                when (resultCode) {
                    Activity.RESULT_OK -> { /* log SENT */ }
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> { /* log FAILED */ }
                    SmsManager.RESULT_ERROR_NO_SERVICE -> { /* log FAILED - no service */ }
                    SmsManager.RESULT_ERROR_RADIO_OFF -> { /* log FAILED - radio off */ }
                }
            }
            "SMS_DELIVERED" -> {
                if (resultCode == Activity.RESULT_OK) { /* log DELIVERED */ }
            }
        }
    }
}
