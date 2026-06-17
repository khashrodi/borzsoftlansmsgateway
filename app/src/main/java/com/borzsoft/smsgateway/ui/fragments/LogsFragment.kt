package com.borzsoft.smsgateway.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.borzsoft.smsgateway.databinding.FragmentLogsBinding
import com.borzsoft.smsgateway.ui.adapters.SmsLogAdapter
import com.borzsoft.smsgateway.ui.viewmodels.GatewayViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogsFragment : Fragment() {

    private var _b: FragmentLogsBinding? = null
    private val b get() = _b!!
    private val vm: GatewayViewModel by activityViewModels()
    private lateinit var adapter: SmsLogAdapter

    override fun onCreateView(inf: LayoutInflater, container: ViewGroup?, s: Bundle?) =
        FragmentLogsBinding.inflate(inf, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        adapter = SmsLogAdapter()
        b.rvLogs.layoutManager = LinearLayoutManager(requireContext())
        b.rvLogs.adapter = adapter

        b.btnClearLogs.setOnClickListener {
            vm.clearLogs()
            Toast.makeText(requireContext(), "Logs cleared", Toast.LENGTH_SHORT).show()
        }

        vm.smsLogs.observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs)
            b.tvLogCount.text = "${logs.size} entries"
            b.tvEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
