package com.borzsoft.smsgateway.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.borzsoft.smsgateway.databinding.FragmentWebAccessBinding
import com.borzsoft.smsgateway.ui.adapters.SessionAdapter
import com.borzsoft.smsgateway.ui.viewmodels.GatewayViewModel
import com.borzsoft.smsgateway.utils.QrUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebAccessFragment : Fragment() {

    private var _b: FragmentWebAccessBinding? = null
    private val b get() = _b!!
    private val vm: GatewayViewModel by activityViewModels()
    private lateinit var sessionAdapter: SessionAdapter

    override fun onCreateView(inf: LayoutInflater, container: ViewGroup?, s: Bundle?) =
        FragmentWebAccessBinding.inflate(inf, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        setupRecycler()
        setupButtons()
        observeData()
        vm.generateNewSession()
    }

    private fun setupRecycler() {
        sessionAdapter = SessionAdapter { token -> vm.revokeSession(token) }
        b.rvSessions.layoutManager = LinearLayoutManager(requireContext())
        b.rvSessions.adapter = sessionAdapter
    }

    private fun setupButtons() {
        b.btnNewQr.setOnClickListener { vm.generateNewSession() }

        b.btnRevokeAll.setOnClickListener {
            vm.revokeAllSessions()
            Toast.makeText(requireContext(), "All sessions revoked", Toast.LENGTH_SHORT).show()
        }

        b.btnCopyToken.setOnClickListener {
            val token = vm.currentToken.value ?: return@setOnClickListener
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("BorzSoft Token", token))
            Toast.makeText(requireContext(), "Token copied!", Toast.LENGTH_SHORT).show()
        }

        b.btnCopyUrl.setOnClickListener {
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("BorzSoft URL", vm.gatewayUrl))
            Toast.makeText(requireContext(), "URL copied!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        vm.currentQrData.observe(viewLifecycleOwner) { qrData ->
            if (qrData != null) {
                val bmp = QrUtils.generate(qrData, 600)
                b.ivQrCode.setImageBitmap(bmp)
                b.cardQr.visibility = View.VISIBLE
            }
        }

        vm.currentToken.observe(viewLifecycleOwner) { token ->
            b.tvToken.text = token ?: "—"
            b.tvGatewayUrl.text = vm.gatewayUrl
        }

        vm.activeSessions.observe(viewLifecycleOwner) { sessions ->
            sessionAdapter.submitList(sessions)
            b.tvSessionCount.text = "${sessions.size} active session${if (sessions.size != 1) "s" else ""}"
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
