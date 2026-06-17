package com.borzsoft.smsgateway.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity
import com.borzsoft.smsgateway.databinding.ActivitySplashBinding
import com.borzsoft.smsgateway.security.DeviceLock

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var b: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(b.root)

        animateLogo()

        Handler(Looper.getMainLooper()).postDelayed({
            val next = if (DeviceLock.isAuthorized(this)) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, UnauthorizedActivity::class.java)
            }
            startActivity(next)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2200)
    }

    private fun animateLogo() {
        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 700 }
        val scale = ScaleAnimation(0.7f, 1f, 0.7f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply { duration = 700 }

        val set = AnimationSet(true).apply {
            addAnimation(fadeIn)
            addAnimation(scale)
        }
        b.splashLogo.startAnimation(set)
        b.splashAppname.startAnimation(AlphaAnimation(0f, 1f).apply { duration = 900; startOffset = 500 })
        b.splashTagline.startAnimation(AlphaAnimation(0f, 1f).apply { duration = 900; startOffset = 800 })
        b.splashVersion.startAnimation(AlphaAnimation(0f, 1f).apply { duration = 900; startOffset = 1100 })
    }
}
