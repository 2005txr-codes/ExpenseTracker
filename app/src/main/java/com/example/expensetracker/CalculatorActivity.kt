package com.example.expensetracker

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class CalculatorActivity : BaseActivity() {
    private lateinit var display: TextView
    private var current = ""
    private var acc: Double? = null
    private var op: String? = null
    private var fresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(24, 24, 24, 24)

        val title = TextView(this)
        title.text = "Calculator"
        title.textSize = 26f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setPadding(4, (8 * d).toInt(), 4, 0)
        root.addView(title)

        display = TextView(this)
        display.textSize = 44f
        display.gravity = Gravity.END
        display.text = "0"
        display.setPadding(16, 32, 16, 32)
        root.addView(display)

        val ops = listOf("/", "*", "-", "+", "=")
        val rows = listOf(
            listOf("7", "8", "9", "/"), listOf("4", "5", "6", "*"),
            listOf("1", "2", "3", "-"), listOf("C", "0", ".", "+"), listOf("=")
        )
        for (r in rows) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            root.addView(row, LinearLayout.LayoutParams(-1, 0, 1f))
            for (label in r) {
                val b = MaterialButton(this)
                b.text = label
                b.textSize = 22f
                b.isAllCaps = false
                b.cornerRadius = 40
                b.backgroundTintList = ColorStateList.valueOf(if (label in ops) C.ACCENT else C.CARD2)
                b.setTextColor(
                    when {
                        label == "C" -> C.RED
                        label in ops -> C.ON_ACCENT
                        else -> C.TEXT
                    }
                )
                b.setOnClickListener { press(label) }
                val lp = LinearLayout.LayoutParams(0, -1, 1f)
                lp.setMargins(6, 6, 6, 6)
                row.addView(b, lp)
            }
        }
        setContentView(root)
        Nav.floatingBack(this)   // back button, bottom-right corner
    }

    private fun press(k: String) {
        when {
            k == "C" -> { current = ""; acc = null; op = null; fresh = false; display.text = "0" }
            k.length == 1 && (k[0].isDigit() || k == ".") -> {
                if (fresh) { current = ""; fresh = false }
                if (k == "." && current.contains(".")) return
                current += k
                display.text = current
            }
            k == "=" -> {
                val a = acc; val o = op; val b = current.toDoubleOrNull()
                if (a != null && o != null && b != null) {
                    val res = calc(a, b, o)
                    display.text = res
                    acc = null; op = null
                    current = if (res == "Error") "" else res
                    fresh = true
                }
            }
            else -> { // + - * /
                val b = current.toDoubleOrNull()
                if (b != null) {
                    val a = acc; val o = op
                    if (a != null && o != null) {
                        val res = calc(a, b, o)
                        if (res == "Error") { press("C"); display.text = "Error"; return }
                        acc = res.toDouble()
                    } else acc = b
                    display.text = fmt(acc!!)
                }
                if (acc != null) { op = k; current = ""; fresh = false }
            }
        }
    }

    private fun calc(a: Double, b: Double, o: String): String {
        val r = when (o) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            else -> if (b == 0.0) return "Error" else a / b
        }
        return fmt(r)
    }

    private fun fmt(d: Double) = if (d % 1.0 == 0.0 && Math.abs(d) < 1e15) d.toLong().toString() else d.toString()
}