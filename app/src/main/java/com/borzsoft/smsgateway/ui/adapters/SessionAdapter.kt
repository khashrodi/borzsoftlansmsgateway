package com.borzsoft.smsgateway.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.borzsoft.smsgateway.databinding.ItemSessionBinding
import com.borzsoft.smsgateway.db.entity.Session
import com.borzsoft.smsgateway.utils.DateUtils

class SessionAdapter(
    private val onRevoke: (String) -> Unit
) : ListAdapter<Session, SessionAdapter.VH>(DIFF) {

    inner class VH(val b: ItemSessionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): VH =
        VH(ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(vh: VH, pos: Int) {
        val session = getItem(pos)
        with(vh.b) {
            tvIp.text = session.clientIp.let { if (it == "*") "Any IP" else it }
            tvCreated.text = "Connected: ${DateUtils.time(session.createdAt)}"
            tvExpires.text = "Expires: ${DateUtils.time(session.expiresAt)}"
            tvMsgSent.text = "${session.messagesSent} messages sent"
            btnRevoke.setOnClickListener { onRevoke(session.token) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Session>() {
            override fun areItemsTheSame(a: Session, b: Session) = a.token == b.token
            override fun areContentsTheSame(a: Session, b: Session) = a == b
        }
    }
}
