package com.example.expensetracker

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.Locale

class AllocationActivity : AppCompatActivity() {
    private lateinit var db: Db
    private lateinit var list: ListView
    private var cats = listOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allocation)
        Nav.back(this, "Category allowances")
        db = Db(this)
        list = findViewById(R.id.listAlloc)
        list.setOnItemClickListener { _, _, pos, _ ->
            if (pos in cats.indices) askAllowance(cats[pos])
        }
        findViewById<Button>(R.id.btnBudgetSet).setOnClickListener { Nav.askBudget(this) { load() } }
        load()
    }

    private fun load() {
        cats = db.categories("expense", true)
        val month = monthKey(Calendar.getInstance())
        val budget = Prefs.budget(this)
        val allocated = cats.sumOf { it.allowance }

        findViewById<TextView>(R.id.tvSummary).text =
            if (budget > 0)
                "Monthly budget ${money(budget)}\nAllocated ${money(allocated)}   •   Unallocated ${money(budget - allocated)}"
            else
                "No monthly budget set. Set one so allowances are checked against it.\nAllocated ${money(allocated)}"

        val rows = cats.map { c ->
            val spent = db.spentInMonth(c.id, month)
            if (c.allowance > 0) {
                val left = c.allowance - spent
                Row(
                    c.icon, c.name, "Spent ${money(spent)} of ${money(c.allowance)}",
                    if (left >= 0) "${money(left)} left" else "Over by ${money(-left)}",
                    if (left >= 0) C.GREEN else C.RED
                )
            } else {
                Row(c.icon, c.name, "Spent ${money(spent)}  •  no limit", "Set", C.ACCENT)
            }
        }.ifEmpty { listOf(Row("📭", "No expense categories selected", "Choose some in Menu > Categories", "", C.MUTED)) }
        list.adapter = RowAdapter(this, rows)
    }

    private fun askAllowance(c: Category) {
        val et = EditText(this)
        et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        et.hint = "Monthly allowance"
        if (c.allowance > 0) et.setText(String.format(Locale.US, "%.2f", c.allowance))

        AlertDialog.Builder(this)
            .setTitle("${c.name} allowance")
            .setMessage("The most you can spend on this category each month.")
            .setView(et)
            .setPositiveButton("Save") { _, _ ->
                val v = et.text.toString().toDoubleOrNull() ?: 0.0
                val budget = Prefs.budget(this)
                val others = db.totalAllowances(c.id)
                if (budget > 0 && others + v > budget) {
                    val available = (budget - others).coerceAtLeast(0.0)
                    AlertDialog.Builder(this).setTitle("Insufficient budget")
                        .setMessage(
                            "Your monthly budget is ${money(budget)} and your other categories already use " +
                                    "${money(others)}.\nOnly ${money(available)} is left to allocate."
                        )
                        .setPositiveButton("OK", null).show()
                } else {
                    db.setAllowance(c.id, v)
                    load()
                }
            }
            .setNeutralButton("Remove limit") { _, _ -> db.setAllowance(c.id, 0.0); load() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}