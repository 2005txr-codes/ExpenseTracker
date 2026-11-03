package com.example.expensetracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView

class ReminderActivity : BaseActivity() {
    private lateinit var list: ListView
    private var items = listOf<Reminder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)
        Nav.back(this, "Reminders")
        list = findViewById(R.id.listReminders)

        findViewById<Button>(R.id.btnAddReminder).setOnClickListener {
            startActivity(Intent(this, ReminderEditActivity::class.java))
        }
        list.setOnItemClickListener { _, _, pos, _ ->
            if (pos in items.indices) {
                startActivity(Intent(this, ReminderEditActivity::class.java).putExtra("id", items[pos].id))
            }
        }
        list.setOnItemLongClickListener { _, _, pos, _ ->
            if (pos in items.indices) {
                val r = items[pos]
                AlertDialog.Builder(this).setTitle("Delete \"${r.title}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        ReminderScheduler.cancel(this, r.id)
                        ReminderStore.delete(this, r.id)
                        load()
                    }
                    .setNegativeButton("Cancel", null).show()
            }
            true
        }
        Notifier.requestPermission(this)
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        items = ReminderStore.all(this)
        val rows = items.map { r ->
            Row(
                "🔔", r.title,
                ReminderStore.describe(r) + "  •  " + r.message.take(36),
                if (r.enabled) "On" else "Off",
                if (r.enabled) C.GREEN else C.MUTED
            )
        }.ifEmpty {
            listOf(Row("📭", "No reminders yet", "Tap \"Add reminder\" to create one", "", C.MUTED))
        }
        list.adapter = RowAdapter(this, rows)
    }
}