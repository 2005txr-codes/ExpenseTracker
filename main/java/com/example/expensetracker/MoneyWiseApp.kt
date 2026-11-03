package com.example.expensetracker

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle

/** Remembers whether the start-up animation has already played in this process. */
object AppState {
    var splashShown = false
}

class MoneyWiseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var started = 0
            private var wasInBackground = false

            override fun onActivityStarted(activity: Activity) {
                val returning = started == 0 && wasInBackground
                started++
                if (activity is SplashActivity) {
                    wasInBackground = false
                    return
                }
                // show the animation on a cold start that skipped it (e.g. notification tap)
                // and every time the app comes back from the background
                if (!AppState.splashShown || returning) {
                    wasInBackground = false
                    activity.startActivity(
                        Intent(activity, SplashActivity::class.java).putExtra("overlay", true)
                    )
                    activity.overridePendingTransition(0, 0)
                }
            }

            override fun onActivityStopped(activity: Activity) {
                started--
                if (started == 0 && !activity.isChangingConfigurations) wasInBackground = true
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}