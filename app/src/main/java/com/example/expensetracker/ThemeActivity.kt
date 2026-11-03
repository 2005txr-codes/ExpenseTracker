package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.ListView

class ThemeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_themes)
        Nav.back(this, "Themes")

        val current = Themes.index(this)
        val rows = Themes.ALL.mapIndexed { i, t ->
            Row("", t.name, t.desc, if (i == current) "✓ Active" else "", C.ACCENT, t.swatch)
        }
        val list = findViewById<ListView>(R.id.listThemes)
        list.adapter = RowAdapter(this, rows)
        list.setOnItemClickListener { _, _, pos, _ ->
            if (pos != current) {
                Themes.set(this, pos)
                // restart on the dashboard so every screen picks up the new theme
                val i = Intent(this, MainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(i)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }
}