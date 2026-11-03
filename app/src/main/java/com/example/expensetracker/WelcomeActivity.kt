package com.example.expensetracker

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

class WelcomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        findViewById<TextView>(R.id.welcomeName).text = Nav.brand()

        val rows = listOf(
            Row("📊", "Track every expense", "Income and spending, sorted by category", "", C.MUTED),
            Row("🎯", "Budgets and allowances", "Set limits so you never overspend", "", C.MUTED),
            Row("📅", "Calendar and reports", "See each day and your monthly breakdown", "", C.MUTED),
            Row("🎨", "Make it yours", "Pick a theme below, change it anytime in Menu", "", C.MUTED)
        )
        findViewById<ListView>(R.id.listFeatures).adapter = RowAdapter(this, rows)
        buildSwatches()

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("welcome_seen", true).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun buildSwatches() {
        val row = findViewById<LinearLayout>(R.id.swatchRow)
        row.removeAllViews()
        val d = resources.displayMetrics.density
        val current = Themes.index(this)
        Themes.ALL.forEachIndexed { i, t ->
            val v = View(this)
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setColor(t.swatch)
            if (i == current) g.setStroke((3 * d).toInt(), C.TEXT)
            v.background = g
            v.setOnClickListener {
                if (i != current) {
                    Themes.set(this, i)
                    recreate()
                }
            }
            val lp = LinearLayout.LayoutParams((44 * d).toInt(), (44 * d).toInt())
            lp.setMargins((8 * d).toInt(), 0, (8 * d).toInt(), 0)
            row.addView(v, lp)
        }
    }
}