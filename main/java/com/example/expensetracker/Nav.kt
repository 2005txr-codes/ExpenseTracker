package com.example.expensetracker

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout


/** Colours of the active theme. Filled in by C.load() when each screen starts. */
object C {
    var BG = Color.parseColor("#0A0D14")
    var CARD = Color.parseColor("#141925")
    var CARD2 = Color.parseColor("#1C2230")
    var ACCENT = Color.parseColor("#2F80FF")
    var ON_ACCENT = Color.WHITE
    var TEXT = Color.WHITE
    var MUTED = Color.parseColor("#8A93A6")
    var CHART = intArrayOf(
        0xFF2F80FF.toInt(), 0xFFFF9F1C.toInt(), 0xFF2ED47A.toInt(),
        0xFFFF4D6A.toInt(), 0xFFA66CFF.toInt(), 0xFF22D3EE.toInt()
    )
    val RED = Color.parseColor("#E5556E")
    val GREEN = Color.parseColor("#3CC98B")
    val WHITE = Color.WHITE

    fun load(c: Context) {
        BG = attr(c, R.attr.exBg)
        CARD = attr(c, R.attr.exCard)
        CARD2 = attr(c, R.attr.exCard2)
        ACCENT = attr(c, R.attr.exAccent)
        ON_ACCENT = attr(c, R.attr.exOnAccent)
        TEXT = attr(c, R.attr.exText)
        MUTED = attr(c, R.attr.exMuted)
        CHART = intArrayOf(
            attr(c, R.attr.exC1), attr(c, R.attr.exC2), attr(c, R.attr.exC3),
            attr(c, R.attr.exC4), attr(c, R.attr.exC5), attr(c, R.attr.exC6)
        )
    }

    private fun attr(c: Context, id: Int): Int {
        val tv = TypedValue()
        c.theme.resolveAttribute(id, tv, true)
        return tv.data
    }
}

/** tint != null draws the icon as a solid coloured circle (used by the theme picker). */
data class Row(
    val icon: String, val title: String, val sub: String,
    val amount: String, val color: Int, val tint: Int? = null
)

/** Card-style list rows used on every list screen. */
class RowAdapter(ctx: Context, private val rows: List<Row>) : BaseAdapter() {
    private val inf = LayoutInflater.from(ctx)
    override fun getCount() = rows.size
    override fun getItem(position: Int) = rows[position]
    override fun getItemId(position: Int) = position.toLong()
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView ?: inf.inflate(R.layout.item_row, parent, false)
        val r = rows[position]
        val icon = v.findViewById<TextView>(R.id.rowIcon)
        icon.text = r.icon
        val t = r.tint
        if (t != null) {
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setColor(t)
            icon.background = g
        } else {
            icon.setBackgroundResource(R.drawable.bg_icon)
        }
        v.findViewById<TextView>(R.id.rowTitle).text = r.title
        v.findViewById<TextView>(R.id.rowSub).text = r.sub
        val amt = v.findViewById<TextView>(R.id.rowAmount)
        amt.text = r.amount
        amt.setTextColor(r.color)
        return v
    }
}

/** Icon grid used to pick categories. `selected` holds the ids that are highlighted. */
class CatAdapter(ctx: Context, private val cats: List<Category>, private val selected: Set<Long>) : BaseAdapter() {
    private val inf = LayoutInflater.from(ctx)
    override fun getCount() = cats.size
    override fun getItem(position: Int) = cats[position]
    override fun getItemId(position: Int) = cats[position].id
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView ?: inf.inflate(R.layout.item_cat, parent, false)
        val c = cats[position]
        val on = c.id in selected
        val icon = v.findViewById<TextView>(R.id.catIcon)
        icon.text = c.icon
        icon.isActivated = on
        val name = v.findViewById<TextView>(R.id.catName)
        name.text = c.name
        name.setTextColor(if (on) C.TEXT else C.MUTED)
        return v
    }
}

object Nav {
    /** Wires the bottom bar. selected: 0 Home, 1 Calendar, 2 Report, 3 Menu */
    fun setup(a: Activity, selected: Int) {
        val ids = listOf(R.id.navHome, R.id.navCal, R.id.navReport, R.id.navMenu)
        val targets = listOf<Class<*>>(
            MainActivity::class.java, CalendarActivity::class.java,
            ReportActivity::class.java, MenuActivity::class.java
        )
        ids.forEachIndexed { i, id ->
            val v = a.findViewById<LinearLayout>(id)
            val on = i == selected
            v.getChildAt(0).alpha = if (on) 1f else 0.5f
            (v.getChildAt(1) as TextView).setTextColor(if (on) C.ACCENT else C.MUTED)
            v.setOnClickListener {
                if (on) return@setOnClickListener
                val intent = Intent(a, targets[i])
                if (i == 0) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                else if (selected != 0) a.finish()
                a.startActivity(intent)
                a.overridePendingTransition(0, 0)
            }
        }
    }

    /** Big back button + title for sub-screens (uses the header_back layout). */
    /** Screen title + floating back button in the bottom-right corner. */
    fun back(a: Activity, title: String) {
        a.findViewById<TextView>(R.id.tvHeader).text = title
        floatingBack(a)
    }

    /** Large "← Back" button floating in the bottom-right corner. */
    fun floatingBack(a: Activity) {
        val d = a.resources.displayMetrics.density
        val content = a.findViewById<ViewGroup>(android.R.id.content)

        // leave room at the bottom so the button never covers content
        val root = content.getChildAt(0)
        if (root != null) {
            root.setPadding(
                root.paddingLeft, root.paddingTop, root.paddingRight,
                root.paddingBottom + (80 * d).toInt()
            )
        }

        val b = TextView(a)
        b.text = "←   Back"
        b.textSize = 17f
        b.typeface = Typeface.DEFAULT_BOLD
        b.setTextColor(C.ON_ACCENT)
        b.gravity = Gravity.CENTER
        b.setBackgroundResource(R.drawable.bg_back_fab)
        b.setPadding((24 * d).toInt(), 0, (28 * d).toInt(), 0)
        b.elevation = 8 * d
        b.contentDescription = "Back"
        b.setOnClickListener { a.finish() }

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, (56 * d).toInt(), Gravity.BOTTOM or Gravity.END
        )
        lp.setMargins(0, 0, (20 * d).toInt(), (20 * d).toInt())
        content.addView(b, lp)
    }

    /** Expense / Income segmented toggle styling. */
    fun toggle(income: Boolean, exp: TextView, inc: TextView) {
        exp.setBackgroundResource(if (!income) R.drawable.bg_toggle_on else R.drawable.bg_toggle_off)
        inc.setBackgroundResource(if (income) R.drawable.bg_toggle_on else R.drawable.bg_toggle_off)
        exp.setTextColor(if (!income) C.ON_ACCENT else C.MUTED)
        inc.setTextColor(if (income) C.ON_ACCENT else C.MUTED)
    }

    /** "MoneyWise" with "Wise" in the accent colour. */
    fun brand(): CharSequence {
        val s = SpannableString("MoneyWise")
        s.setSpan(ForegroundColorSpan(C.ACCENT), 5, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }

    fun askBudget(a: Activity, onDone: () -> Unit) {
        val et = EditText(a)
        et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        et.hint = "Monthly budget"
        val cur = Prefs.budget(a)
        if (cur > 0) et.setText(String.format(Locale.US, "%.2f", cur))
        AlertDialog.Builder(a).setTitle("Monthly budget").setView(et)
            .setPositiveButton("Save") { _, _ ->
                Prefs.setBudget(a, et.text.toString().toDoubleOrNull() ?: 0.0)
                onDone()
            }.setNegativeButton("Cancel", null).show()
    }
}