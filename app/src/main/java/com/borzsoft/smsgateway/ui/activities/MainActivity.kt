package com.borzsoft.smsgateway.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.borzsoft.smsgateway.R
import com.borzsoft.smsgateway.databinding.ActivityMainBinding
import com.borzsoft.smsgateway.service.GatewayService
import com.borzsoft.smsgateway.ui.fragments.DashboardFragment
import com.borzsoft.smsgateway.ui.fragments.LogsFragment
import com.borzsoft.smsgateway.ui.fragments.SendFragment
import com.borzsoft.smsgateway.ui.fragments.WebAccessFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.entries.filter { !it.value }.map { it.key }
        if (denied.isNotEmpty()) {
            Toast.makeText(this, "Some permissions denied. SMS may not work.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        checkPermissions()
        setupNavigation()

        if (savedInstanceState == null) {
            showFragment(DashboardFragment(), "dashboard")
        }
    }

    private fun setupNavigation() {
        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { showFragment(DashboardFragment(), "dashboard"); true }
                R.id.nav_send -> { showFragment(SendFragment(), "send"); true }
                R.id.nav_web -> { showFragment(WebAccessFragment(), "web"); true }
                R.id.nav_logs -> { showFragment(LogsFragment(), "logs"); true }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
    }

    private fun checkPermissions() {
        val needed = buildList {
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    fun startGateway() {
        ContextCompat.startForegroundService(this, Intent(this, GatewayService::class.java))
    }

    fun stopGateway() {
        val intent = Intent(this, GatewayService::class.java).apply {
            action = GatewayService.ACTION_STOP
        }
        startService(intent)
    }
}
