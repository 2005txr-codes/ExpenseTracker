package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView

class SplashActivity : BaseActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var overlay = false   // true = shown on top of the screen the user was on

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppState.splashShown = true
        overlay = intent.getBooleanExtra("overlay", false)
        setContentView(R.layout.activity_splash)

        findViewById<TextView>(R.id.splashName).text = Nav.brand()

        val logo = findViewById<View>(R.id.splashLogo)
        logo.alpha = 0f; logo.scaleX = 0.6f; logo.scaleY = 0.6f
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700)
            .setInterpolator(OvershootInterpolator()).start()

        fadeIn(findViewById(R.id.splashName), 300)
        fadeIn(findViewById(R.id.splashTag), 600)
        fadeIn(findViewById(R.id.splashLoader), 900)
        fadeIn(findViewById(R.id.splashLoading), 1000)

        handler.postDelayed({ goNext() }, if (overlay) 2200L else 2800L)
    }

    private fun fadeIn(v: View, delay: Long) {
        v.alpha = 0f
        v.translationY = 24f
        v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(600).start()
    }

    private fun goNext() {
        if (overlay) {
            // go back to whatever screen the user was on
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            return
        }
        val seen = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("welcome_seen", false)
        startActivity(Intent(this, if (seen) MainActivity::class.java else WelcomeActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}