package com.borzsoft.smsgateway.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.borzsoft.smsgateway.databinding.ActivitySettingsBinding
import com.borzsoft.smsgateway.ui.fragments.SettingsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private lateinit var b: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        b.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(b.settingsContainer.id, SettingsFragment())
                .commit()
        }
    }
}
