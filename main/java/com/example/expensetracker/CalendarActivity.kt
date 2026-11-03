package com.example.expensetracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class CalendarActivity : AppCompatActivity() {
    private lateinit var db: Db

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        Nav.setup(this, 1)
        db = Db(this)
        val cv = findViewById<CalendarView>(R.id.calView)
        cv.maxDate = System.currentTimeMillis()
        cv.minDate = cutoffCal().timeInMillis
        cv.setOnDateChangeListener { _, y, m, d ->
            show(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
        }
        show(today())
    }

    private fun show(date: String) {
        val txns = db.txnsForDate(date)
        val inc = txns.filter { it.type == "income" }.sumOf { it.amount }
        val exp = txns.filter { it.type == "expense" }.sumOf { it.amount }
        findViewById<TextView>(R.id.tvDay).text =
            "$date   •   Income ${money(inc)}   •   Expenses ${money(exp)}"
        val rows = if (txns.isEmpty()) listOf(Row("📭", "No records on this day", "", "", C.MUTED))
        else txns.map {
            val isInc = it.type == "income"
            Row(it.icon, if (it.note.isEmpty()) it.catName else it.note, it.catName,
                (if (isInc) "+" else "-") + money(it.amount), if (isInc) C.GREEN else C.RED)
        }
        findViewById<ListView>(R.id.listDay).adapter = RowAdapter(this, rows)
    }
}