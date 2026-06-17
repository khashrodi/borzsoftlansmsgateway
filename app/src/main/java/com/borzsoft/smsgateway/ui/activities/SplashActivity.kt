package com.borzsoft.smsgateway.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.borzsoft.smsgateway.R
import com.borzsoft.smsgateway.security.DeviceLock

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splash_logo)
        val tagline = findViewById<TextView>(R.id.splash_tagline)

        val fade = AlphaAnimation(0f, 1f).apply { duration = 800 }
        logo.startAnimation(fade)
        tagline.startAnimation(fade)

        Handler(Looper.getMainLooper()).postDelayed({
            if (DeviceLock.isAuthorized(this)) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, UnauthorizedActivity::class.java))
            }
            finish()
        }, 2000)
    }
}
