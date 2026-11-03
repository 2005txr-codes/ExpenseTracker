package com.example.expensetracker

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import java.util.Locale

class AddActivity : BaseActivity() {
    private lateinit var db: Db
    private lateinit var btnDate: Button
    private lateinit var cat: Category
    private var date = today()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        db = Db(this)

        val id = intent.getLongExtra("catId", -1)
        val found = db.categories().firstOrNull { it.id == id }
        if (found == null) { finish(); return }
        cat = found
        val isIncome = cat.type == "income"

        Nav.back(this, if (isIncome) "Add Income" else "Add Expense")

        val icon = findViewById<TextView>(R.id.tvCatIcon)
        icon.text = cat.icon
        icon.isActivated = true
        findViewById<TextView>(R.id.tvCatName).text = cat.name
        findViewById<TextView>(R.id.tvCatInfo).text =
            if (!isIncome && cat.allowance > 0) {
                val spent = db.spentInMonth(cat.id, monthKey(Calendar.getInstance()))
                val left = (cat.allowance - spent).coerceAtLeast(0.0)
                "Expense  •  Allowance ${money(cat.allowance)}  •  ${money(left)} left this month"
            } else cap(cat.type)

        btnDate = findViewById(R.id.btnDate)
        btnDate.text = "📅  $date"
        btnDate.setOnClickListener {
            val p = date.split("-")
            val dp = DatePickerDialog(this, { _, y, m, d ->
                date = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                btnDate.text = "📅  $date"
            }, p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
            dp.datePicker.maxDate = System.currentTimeMillis()
            dp.datePicker.minDate = cutoffCal().timeInMillis   // only last 3 months
            dp.show()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val amt = findViewById<EditText>(R.id.etAmount).text.toString().toDoubleOrNull()
            if (amt == null || amt <= 0) { toast("Enter a valid amount"); return@setOnClickListener }

            // allocation check: only expenses are limited
            if (!isIncome) {
                val problem = checkBudget(amt)
                if (problem != null) {
                    AlertDialog.Builder(this).setTitle("Insufficient budget")
                        .setMessage(problem).setPositiveButton("OK", null).show()
                    return@setOnClickListener
                }
            }

            val note = findViewById<EditText>(R.id.etNote).text.toString().trim()
            db.addTxn(cat.id, amt, note, date)
            toast("Saved")
            finish()
        }
    }

    /** Returns an explanation if the expense is not allowed, or null if it is fine. */
    private fun checkBudget(amt: Double): String? {
        val month = date.substring(0, 7)

        // 1) category allowance
        if (cat.allowance > 0) {
            val spent = db.spentInMonth(cat.id, month)
            if (spent + amt > cat.allowance) {
                val left = (cat.allowance - spent).coerceAtLeast(0.0)
                return "Your ${cat.name} allowance is ${money(cat.allowance)} per month and only " +
                        "${money(left)} is left.\nThis expense is ${money(amt)}."
            }
        }

        // 2) overall monthly budget
        val budget = Prefs.budget(this)
        if (budget > 0) {
            val spent = db.monthTotal("expense", month)
            if (spent + amt > budget) {
                val left = (budget - spent).coerceAtLeast(0.0)
                return "Your monthly budget is ${money(budget)} and only ${money(left)} is left.\n" +
                        "This expense is ${money(amt)}."
            }
        }
        return null
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}