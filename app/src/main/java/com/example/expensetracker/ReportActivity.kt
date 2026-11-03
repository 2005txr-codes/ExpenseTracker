package com.example.expensetracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportActivity : AppCompatActivity() {
    private lateinit var db: Db
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private var offset = 0   // 0 = this month, -1 = last month ... down to -3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        Nav.setup(this, 2)
        db = Db(this)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnPrev.setOnClickListener { offset--; show() }
        btnNext.setOnClickListener { offset++; show() }
        show()
    }

    private fun show() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        findViewById<TextView>(R.id.tvMonth).text =
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        btnPrev.isEnabled = offset > -3
        btnNext.isEnabled = offset < 0

        val rows = db.totalsByCategory(monthKey(cal)).filter { it.total > 0 }
        val exp = rows.filter { it.type == "expense" }.sortedByDescending { it.total }
        val inc = rows.filter { it.type == "income" }.sortedByDescending { it.total }
        val te = exp.sumOf { it.total }
        val ti = inc.sumOf { it.total }

        val slices = exp.take(6).map { it.name to it.total }.toMutableList()
        val rest = exp.drop(6).sumOf { it.total }
        if (rest > 0) slices.add("Others" to rest)
        findViewById<PieChartView>(R.id.pie).setData(slices, money(te))

        val list = mutableListOf<Row>()
        exp.forEach {
            list.add(Row(it.icon, it.name, "Expense  •  ${(it.total / te * 100).toInt()}%", "-" + money(it.total), C.RED))
        }
        inc.forEach {
            list.add(Row(it.icon, it.name, "Income  •  ${(it.total / ti * 100).toInt()}%", "+" + money(it.total), C.GREEN))
        }
        if (list.isEmpty()) list.add(Row("📭", "No records this month", "", "", C.MUTED))
        findViewById<ListView>(R.id.listReport).adapter = RowAdapter(this, list)

        val budget = Prefs.budget(this)
        val budgetTxt = if (budget > 0) "\nBudget ${money(budget)}: ${(te / budget * 100).toInt()}% used" else ""
        findViewById<TextView>(R.id.tvTotals).text =
            "Income ${money(ti)}   •   Expenses ${money(te)}   •   Net ${money(ti - te)}$budgetTxt"
    }
}