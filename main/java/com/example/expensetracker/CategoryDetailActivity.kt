package com.example.expensetracker

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class CategoryDetailActivity : AppCompatActivity() {
    private lateinit var db: Db
    private lateinit var list: ListView
    private var catId = 0L
    private var txns = listOf<Txn>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_detail)
        Nav.back(this, intent.getStringExtra("catName") ?: "Category")
        db = Db(this)
        catId = intent.getLongExtra("catId", 0)
        list = findViewById(R.id.listTxns)

        list.setOnItemClickListener { _, _, pos, _ ->
            if (pos !in txns.indices) return@setOnItemClickListener
            val t = txns[pos]
            AlertDialog.Builder(this).setTitle("Delete this record?")
                .setMessage("${t.date}  ${money(t.amount)}  ${t.note}")
                .setPositiveButton("Delete") { _, _ -> db.deleteTxn(t.id); load() }
                .setNegativeButton("Cancel", null).show()
        }
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            AlertDialog.Builder(this).setTitle("Delete ALL records in this category?")
                .setPositiveButton("Delete all") { _, _ -> db.deleteTxnsForCategory(catId); load() }
                .setNegativeButton("Cancel", null).show()
        }
        load()
    }

    private fun load() {
        txns = db.txnsForCategory(catId)
        findViewById<TextView>(R.id.tvTotal).text = "Total (last 3 months): ${money(txns.sumOf { it.amount })}"
        val rows = if (txns.isEmpty()) listOf(Row("📭", "No records yet", "", "", C.MUTED))
        else txns.map {
            val inc = it.type == "income"
            Row(it.icon, if (it.note.isEmpty()) it.catName else it.note, it.date,
                (if (inc) "+" else "-") + money(it.amount), if (inc) C.GREEN else C.RED)
        }
        list.adapter = RowAdapter(this, rows)
    }
}