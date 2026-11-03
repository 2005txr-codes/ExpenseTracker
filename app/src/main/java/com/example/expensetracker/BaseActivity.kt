package com.example.expensetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** Every screen extends this so the chosen theme is applied before the UI is built. */
open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Themes.styleRes(this))
        super.onCreate(savedInstanceState)
        C.load(this)
        Notifier.ensureChannel(this)   // makes MoneyWise appear in the system notification settings
    }
}