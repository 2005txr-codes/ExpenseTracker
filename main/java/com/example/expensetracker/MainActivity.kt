package com.example.expensetracker

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : BaseActivity() {
    private lateinit var db: Db
    private lateinit var list: ListView
    private lateinit var header: View
    private var items = listOf<CatTotal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = Db(this)
        list = findViewById(R.id.listMain)
        header = layoutInflater.inflate(R.layout.header_main, list, false)
        list.addHeaderView(header, null, false)
        Nav.setup(this, 0)

        header.findViewById<View>(R.id.cardBudget).setOnClickListener { Nav.askBudget(this) { refresh() } }

        // tap a category = add a transaction to it
        list.setOnItemClickListener { _, _, pos, _ ->
            val i = pos - list.headerViewsCount
            if (i in items.indices) {
                startActivity(
                    Intent(this, AddActivity::class.java).putExtra("catId", items[i].id)
                )
            }
        }
        // hold a category = view / delete its records
        list.setOnItemLongClickListener { _, _, pos, _ ->
            val i = pos - list.headerViewsCount
            if (i in items.indices) {
                startActivity(
                    Intent(this, CategoryDetailActivity::class.java)
                        .putExtra("catId", items[i].id).putExtra("catName", items[i].name)
                )
            }
            true
        }

        // first launch: let the user choose their categories
        if (!Prefs.setupDone(this)) startActivity(Intent(this, CategoriesActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        db.deleteOldData()
        refresh()
    }

    private fun refresh() {
        val m = monthKey(Calendar.getInstance())
        items = db.totalsByCategory(m)
        val inc = db.monthTotal("income", m)
        val exp = db.monthTotal("expense", m)

        header.findViewById<TextView>(R.id.tvMonth).text =
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        header.findViewById<TextView>(R.id.tvBalance).text = money(inc - exp)
        header.findViewById<TextView>(R.id.tvIncome).text = money(inc)
        header.findViewById<TextView>(R.id.tvExpenses).text = money(exp)

        // budget
        val budget = Prefs.budget(this)
        val tvBudget = header.findViewById<TextView>(R.id.tvBudget)
        val bar = header.findViewById<ProgressBar>(R.id.progressBudget)
        if (budget <= 0) {
            tvBudget.text = "No budget set - tap to set one"
            tvBudget.setTextColor(C.MUTED)
            bar.progress = 0
        } else {
            bar.progress = (exp / budget * 100).toInt().coerceAtMost(100)
            if (exp > budget) {
                tvBudget.text = "Over budget by ${money(exp - budget)}  (budget ${money(budget)})"
                tvBudget.setTextColor(C.RED)
                bar.progressTintList = ColorStateList.valueOf(C.RED)
            } else {
                tvBudget.text = "Spent ${money(exp)} of ${money(budget)}  -  ${money(budget - exp)} left"
                tvBudget.setTextColor(C.MUTED)
                bar.progressTintList = ColorStateList.valueOf(C.ACCENT)
            }
        }

        // category list with totals
        val rows = items.map {
            val color = when {
                it.total <= 0 -> C.MUTED
                it.type == "income" -> C.GREEN
                else -> C.RED
            }
            val sub = if (it.type == "expense" && it.allowance > 0) {
                val left = it.allowance - it.total
                if (left >= 0) "Limit ${money(it.allowance)}  •  ${money(left)} left"
                else "Limit ${money(it.allowance)}  •  over by ${money(-left)}"
            } else cap(it.type)
            Row(it.icon, it.name, sub, money(it.total), color)
        }
        list.adapter = RowAdapter(this, rows)
    }
}