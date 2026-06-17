package com.borzsoft.smsgateway.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.borzsoft.smsgateway.databinding.ActivityLogsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogsActivity : AppCompatActivity() {
    private lateinit var b: ActivityLogsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        b.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}
