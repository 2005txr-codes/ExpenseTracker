package com.example.expensetracker

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class CategoriesActivity : AppCompatActivity() {
    private lateinit var db: Db
    private lateinit var grid: GridView
    private lateinit var tvExpense: TextView
    private lateinit var tvIncome: TextView
    private var all = listOf<Category>()
    private var shown = listOf<Category>()
    private val selected = mutableSetOf<Long>()
    private var income = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)
        Nav.back(this, "Choose categories")
        db = Db(this)
        grid = findViewById(R.id.grid)
        tvExpense = findViewById(R.id.tvExpense)
        tvIncome = findViewById(R.id.tvIncome)
        val etIcon = findViewById<EditText>(R.id.etIcon)
        val etName = findViewById<EditText>(R.id.etCatName)
        etIcon.setText("🏷️")

        tvExpense.setOnClickListener { income = false; show() }
        tvIncome.setOnClickListener { income = true; show() }

        grid.setOnItemClickListener { _, _, pos, _ ->
            val id = shown[pos].id
            if (!selected.add(id)) selected.remove(id)
            (grid.adapter as BaseAdapter).notifyDataSetChanged()
        }
        grid.setOnItemLongClickListener { _, _, pos, _ ->
            val c = shown[pos]
            if (c.custom) {
                AlertDialog.Builder(this).setTitle("Delete \"${c.name}\"?")
                    .setMessage("This also deletes all its records.")
                    .setPositiveButton("Delete") { _, _ -> saveChecks(); db.deleteCategory(c.id); loadAll() }
                    .setNegativeButton("Cancel", null).show()
            } else {
                Toast.makeText(this, "Preset categories can be unticked, not deleted", Toast.LENGTH_SHORT).show()
            }
            true
        }

        findViewById<Button>(R.id.btnAddCat).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) { etName.error = "Enter a name"; return@setOnClickListener }
            val icon = etIcon.text.toString().trim().ifEmpty { "🏷️" }
            saveChecks()
            db.addCategory(name, icon, if (income) "income" else "expense")
            etName.setText("")
            loadAll()
        }

        findViewById<Button>(R.id.btnSaveCats).setOnClickListener {
            saveChecks()
            if (db.categories(null, true).isEmpty()) {
                Toast.makeText(this, "Pick at least one category", Toast.LENGTH_SHORT).show()
            } else {
                Prefs.setSetupDone(this)
                finish()
            }
        }
        loadAll()
    }

    private fun loadAll() {
        all = db.categories()
        selected.clear()
        selected.addAll(all.filter { it.active }.map { it.id })
        show()
    }

    private fun show() {
        shown = all.filter { (it.type == "income") == income }
        grid.adapter = CatAdapter(this, shown, selected)
        Nav.toggle(income, tvExpense, tvIncome)
    }

    private fun saveChecks() {
        all.forEach { db.setActive(it.id, it.id in selected) }
    }
}