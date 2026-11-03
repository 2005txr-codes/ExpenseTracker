package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.ListView

class MenuActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        Nav.setup(this, 3)
        val rows = listOf(
            Row("🗂️", "Categories", "Choose presets or create your own", "›", C.MUTED),
            Row("🎯", "Category allowances", "Set a spending limit per category", "›", C.MUTED),
            Row("💰", "Monthly budget", "Set how much you plan to spend", "›", C.MUTED),
            Row("🎨", "Themes", "Midnight Blue, Teal Slate, Black Gold, Rose Velvet", "›", C.MUTED),
            Row("🧮", "Calculator", "Quick calculations", "›", C.MUTED),
            Row("🔔", "Reminder", "Customise your daily reminder", "›", C.MUTED)
        )
        val list = findViewById<ListView>(R.id.listMenu)
        list.adapter = RowAdapter(this, rows)
        list.setOnItemClickListener { _, _, pos, _ ->
            when (pos) {
                0 -> startActivity(Intent(this, CategoriesActivity::class.java))
                1 -> startActivity(Intent(this, AllocationActivity::class.java))
                2 -> Nav.askBudget(this) { }
                3 -> startActivity(Intent(this, ThemeActivity::class.java))
                4 -> startActivity(Intent(this, CalculatorActivity::class.java))
                else -> startActivity(Intent(this, ReminderActivity::class.java))
            }
        }
    }
}