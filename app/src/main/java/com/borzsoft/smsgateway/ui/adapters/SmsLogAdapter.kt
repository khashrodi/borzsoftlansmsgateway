package com.borzsoft.smsgateway.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.borzsoft.smsgateway.R
import com.borzsoft.smsgateway.databinding.ItemSmsLogBinding
import com.borzsoft.smsgateway.db.entity.SmsLog
import com.borzsoft.smsgateway.utils.DateUtils

class SmsLogAdapter : ListAdapter<SmsLog, SmsLogAdapter.VH>(DIFF) {

    inner class VH(val b: ItemSmsLogBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): VH =
        VH(ItemSmsLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(vh: VH, pos: Int) {
        val log = getItem(pos)
        val ctx = vh.itemView.context
        with(vh.b) {
            tvPhone.text = log.phone
            tvMessage.text = log.message
            tvSource.text = "[${log.source}] SIM: ${log.sim.uppercase()}"
            tvTime.text = DateUtils.time(log.sentAt)
            tvSenderIp.text = "from ${log.senderIp}"

            val (statusText, statusColor) = when (log.status) {
                "SENT"      -> "SENT" to R.color.status_online
                "DELIVERED" -> "DELIVERED" to R.color.status_online
                "FAILED"    -> "FAILED" to R.color.status_offline
                else        -> "PENDING" to R.color.status_pending
            }
            tvStatus.text = statusText
            tvStatus.setTextColor(ContextCompat.getColor(ctx, statusColor))

            if (!log.errorMsg.isNullOrBlank()) {
                tvError.visibility = View.VISIBLE
                tvError.text = log.errorMsg
            } else {
                tvError.visibility = View.GONE
            }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SmsLog>() {
            override fun areItemsTheSame(a: SmsLog, b: SmsLog) = a.id == b.id
            override fun areContentsTheSame(a: SmsLog, b: SmsLog) = a == b
        }
    }
}
