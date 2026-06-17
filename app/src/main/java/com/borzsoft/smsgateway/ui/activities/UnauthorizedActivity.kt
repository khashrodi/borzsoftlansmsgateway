package com.borzsoft.smsgateway.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.borzsoft.smsgateway.databinding.ActivityUnauthorizedBinding

class UnauthorizedActivity : AppCompatActivity() {
    private lateinit var b: ActivityUnauthorizedBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityUnauthorizedBinding.inflate(layoutInflater)
        setContentView(b.root)
    }
}
