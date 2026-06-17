package com.borzsoft.smsgateway.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.borzsoft.smsgateway.R
import com.borzsoft.smsgateway.databinding.FragmentDashboardBinding
import com.borzsoft.smsgateway.service.GatewayService
import com.borzsoft.smsgateway.ui.activities.MainActivity
import com.borzsoft.smsgateway.ui.activities.SettingsActivity
import com.borzsoft.smsgateway.ui.viewmodels.GatewayViewModel
import com.borzsoft.smsgateway.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _b: FragmentDashboardBinding? = null
    private val b get() = _b!!
    private val vm: GatewayViewModel by activityViewModels()

    override fun onCreateView(inf: LayoutInflater, container: ViewGroup?, s: Bundle?) =
        FragmentDashboardBinding.inflate(inf, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeData()
    }

    private fun setupUI() {
        updateStatus()

        b.btnToggle.setOnClickListener {
            if (GatewayService.isRunning) {
                (activity as? MainActivity)?.stopGateway()
            } else {
                (activity as? MainActivity)?.startGateway()
            }
            view?.postDelayed({ updateStatus() }, 500)
        }

        b.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        b.btnRefresh.setOnClickListener { updateStatus() }
    }

    private fun observeData() {
        vm.smsLogs.observe(viewLifecycleOwner) { logs ->
            val today = java.time.LocalDate.now().toString()
            val todayCount = logs.count { it.sentAt.startsWith(today) }
            val sentCount = logs.count { it.status == "SENT" || it.status == "DELIVERED" }
            val failedCount = logs.count { it.status == "FAILED" }
            b.tvSmsToday.text = todayCount.toString()
            b.tvSmsSent.text = sentCount.toString()
            b.tvSmsFailed.text = failedCount.toString()
        }

        vm.activeSessionCount.observe(viewLifecycleOwner) { count ->
            b.tvActiveSessions.text = count.toString()
        }
    }

    private fun updateStatus() {
        val running = GatewayService.isRunning
        val ip = NetworkUtils.getLocalIp()
        val ctx = requireContext()

        b.tvGatewayUrl.text = if (running) "http://$ip:${GatewayService.PORT}" else "Gateway not running"
        b.tvStatusBadge.text = if (running) "● ONLINE" else "● OFFLINE"
        b.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx,
            if (running) R.color.status_online else R.color.status_offline))
        b.btnToggle.text = if (running) "Stop Gateway" else "Start Gateway"
        b.btnToggle.backgroundTintList = ContextCompat.getColorStateList(ctx,
            if (running) R.color.status_offline else R.color.accent)
        b.cardServer.strokeColor = ContextCompat.getColor(ctx,
            if (running) R.color.status_online else R.color.surface_variant)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
