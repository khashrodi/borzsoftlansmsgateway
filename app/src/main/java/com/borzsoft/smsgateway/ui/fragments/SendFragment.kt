package com.borzsoft.smsgateway.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.borzsoft.smsgateway.databinding.FragmentSendBinding
import com.borzsoft.smsgateway.db.AppDatabase
import com.borzsoft.smsgateway.service.SmsEngine
import com.borzsoft.smsgateway.ui.viewmodels.GatewayViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SendFragment : Fragment() {

    private var _b: FragmentSendBinding? = null
    private val b get() = _b!!
    @Inject lateinit var db: AppDatabase

    override fun onCreateView(inf: LayoutInflater, container: ViewGroup?, s: Bundle?) =
        FragmentSendBinding.inflate(inf, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)

        b.etMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val len = s?.length ?: 0
                val parts = if (len <= 160) 1 else (len + 152) / 153
                b.tvCharCount.text = "$len chars • $parts part${if (parts > 1) "s" else ""}"
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, a: Int) {}
        })

        b.btnSend.setOnClickListener { sendSms() }
        b.btnClear.setOnClickListener {
            b.etPhone.text?.clear()
            b.etMessage.text?.clear()
        }
    }

    private fun sendSms() {
        val phone = b.etPhone.text?.toString()?.trim() ?: ""
        val message = b.etMessage.text?.toString()?.trim() ?: ""
        val sim = when (b.rgSim.checkedRadioButtonId) {
            b.rbSim1.id -> "sim1"
            b.rbSim2.id -> "sim2"
            else -> "default"
        }

        if (phone.isEmpty()) { b.etPhone.error = "Required"; return }
        if (message.isEmpty()) { b.etMessage.error = "Required"; return }

        b.btnSend.isEnabled = false
        b.progressBar.visibility = View.VISIBLE
        b.tvResult.visibility = View.GONE

        lifecycleScope.launch {
            val engine = SmsEngine(requireContext(), db.smsLogDao())
            val result = engine.send(phone, message, sim, "app", "APP")

            b.btnSend.isEnabled = true
            b.progressBar.visibility = View.GONE
            b.tvResult.visibility = View.VISIBLE

            if (result.success) {
                b.tvResult.text = "✓ SMS sent successfully (ID: ${result.logId})"
                b.tvResult.setTextColor(resources.getColor(com.borzsoft.smsgateway.R.color.status_online, null))
                b.etPhone.text?.clear()
                b.etMessage.text?.clear()
            } else {
                b.tvResult.text = "✗ Failed: ${result.error}"
                b.tvResult.setTextColor(resources.getColor(com.borzsoft.smsgateway.R.color.status_offline, null))
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
