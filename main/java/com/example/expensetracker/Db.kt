package com.example.expensetracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Category(
    val id: Long, val name: String, val type: String, val icon: String,
    val active: Boolean, val custom: Boolean, val allowance: Double = 0.0
)
data class CatTotal(
    val id: Long, val name: String, val type: String, val icon: String,
    val total: Double, val allowance: Double = 0.0
)
data class Txn(val id: Long, val catName: String, val icon: String, val type: String, val amount: Double, val note: String, val date: String)

fun money(d: Double): String = String.format(Locale.US, "$%,.2f", d)
fun dateStr(c: Calendar): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
fun monthKey(c: Calendar): String = SimpleDateFormat("yyyy-MM", Locale.US).format(c.time)
fun today(): String = dateStr(Calendar.getInstance())
fun cutoffCal(): Calendar = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }
fun cap(s: String): String = s.replaceFirstChar { it.uppercase() }

object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
    fun budget(c: Context): Double = p(c).getFloat("budget", 0f).toDouble()
    fun setBudget(c: Context, v: Double) { p(c).edit().putFloat("budget", v.toFloat()).apply() }
    fun setupDone(c: Context): Boolean = p(c).getBoolean("setup_done", false)
    fun setSetupDone(c: Context) { p(c).edit().putBoolean("setup_done", true).apply() }
}

private val EXPENSE_PRESETS = listOf(
    "Food & Dining" to "🍔", "Groceries" to "🛒", "Transport" to "🚌", "Fuel" to "⛽",
    "Shopping" to "🛍️", "Entertainment" to "🎬", "Bills & Utilities" to "💡", "Rent & Housing" to "🏠",
    "Health" to "💊", "Education" to "🎓", "Travel" to "✈️", "Phone & Internet" to "📱",
    "Subscriptions" to "🔁", "Personal Care" to "💇", "Gifts & Donations" to "🎁", "Pets" to "🐾",
    "Insurance" to "🛡️", "Fitness" to "🏋️", "Other" to "📦"
)
private val INCOME_PRESETS = listOf(
    "Salary" to "💼", "Freelance" to "💻", "Business" to "🏪", "Allowance" to "👛",
    "Investments" to "📈", "Bonus" to "🎉", "Gifts" to "🎁", "Refunds" to "↩️",
    "Rental Income" to "🏘️", "Other" to "💰"
)
// pre-ticked on first launch (user can change them)
private val DEFAULT_ON = setOf(
    "Food & Dining", "Transport", "Shopping", "Entertainment", "Bills & Utilities",
    "Salary", "Allowance", "Other"
)

class Db(ctx: Context) : SQLiteOpenHelper(ctx, "expense.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE categories(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, type TEXT, icon TEXT, active INTEGER DEFAULT 0, custom INTEGER DEFAULT 0, allowance REAL DEFAULT 0)")
        db.execSQL("CREATE TABLE txns(id INTEGER PRIMARY KEY AUTOINCREMENT, cat_id INTEGER, amount REAL, note TEXT, date TEXT)")
        EXPENSE_PRESETS.forEach { insertCat(db, it.first, it.second, "expense", 0, if (it.first in DEFAULT_ON) 1 else 0) }
        INCOME_PRESETS.forEach { insertCat(db, it.first, it.second, "income", 0, if (it.first in DEFAULT_ON) 1 else 0) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        if (oldV < 2) {
            // very old dev schema: reset
            db.execSQL("DROP TABLE IF EXISTS txns")
            db.execSQL("DROP TABLE IF EXISTS categories")
            onCreate(db)
            return
        }
        if (oldV < 3) {
            // keeps existing data, adds the allowance column
            db.execSQL("ALTER TABLE categories ADD COLUMN allowance REAL DEFAULT 0")
        }
    }

    private fun insertCat(db: SQLiteDatabase, name: String, icon: String, type: String, custom: Int, active: Int): Long {
        val v = ContentValues()
        v.put("name", name); v.put("icon", icon); v.put("type", type)
        v.put("custom", custom); v.put("active", active)
        return db.insert("categories", null, v)
    }

    // ---------- categories ----------
    fun addCategory(name: String, icon: String, type: String): Long =
        insertCat(writableDatabase, name, icon, type, 1, 1)

    fun categories(type: String? = null, activeOnly: Boolean = false): List<Category> {
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (type != null) { where.add("type=?"); args.add(type) }
        if (activeOnly) where.add("active=1")
        val out = mutableListOf<Category>()
        readableDatabase.query(
            "categories", null,
            if (where.isEmpty()) null else where.joinToString(" AND "),
            if (args.isEmpty()) null else args.toTypedArray(),
            null, null, "type, custom, id"
        ).use { c ->
            while (c.moveToNext()) out.add(
                Category(
                    c.getLong(0), c.getString(1), c.getString(2), c.getString(3) ?: "🏷️",
                    c.getInt(4) == 1, c.getInt(5) == 1, c.getDouble(6)
                )
            )
        }
        return out
    }

    fun setActive(id: Long, active: Boolean) {
        val v = ContentValues(); v.put("active", if (active) 1 else 0)
        writableDatabase.update("categories", v, "id=?", arrayOf(id.toString()))
    }

    fun deleteCategory(id: Long) {
        writableDatabase.delete("txns", "cat_id=?", arrayOf(id.toString()))
        writableDatabase.delete("categories", "id=? AND custom=1", arrayOf(id.toString()))
    }

    // ---------- allowances ----------
    fun setAllowance(catId: Long, amount: Double) {
        val v = ContentValues(); v.put("allowance", amount)
        writableDatabase.update("categories", v, "id=?", arrayOf(catId.toString()))
    }

    /** Sum of allowances of active expense categories, optionally excluding one category. */
    fun totalAllowances(excludeId: Long = -1): Double {
        readableDatabase.rawQuery(
            "SELECT IFNULL(SUM(allowance),0) FROM categories WHERE active=1 AND type='expense' AND id<>?",
            arrayOf(excludeId.toString())
        ).use { c -> return if (c.moveToFirst()) c.getDouble(0) else 0.0 }
    }

    /** How much has been spent in one category during a month (yyyy-MM). */
    fun spentInMonth(catId: Long, month: String): Double {
        readableDatabase.rawQuery(
            "SELECT IFNULL(SUM(amount),0) FROM txns WHERE cat_id=? AND date LIKE ?",
            arrayOf(catId.toString(), "$month-%")
        ).use { c -> return if (c.moveToFirst()) c.getDouble(0) else 0.0 }
    }

    // ---------- transactions ----------
    fun addTxn(catId: Long, amount: Double, note: String, date: String) {
        val v = ContentValues()
        v.put("cat_id", catId); v.put("amount", amount); v.put("note", note); v.put("date", date)
        writableDatabase.insert("txns", null, v)
    }

    fun deleteTxn(id: Long) { writableDatabase.delete("txns", "id=?", arrayOf(id.toString())) }

    fun deleteTxnsForCategory(catId: Long) {
        writableDatabase.delete("txns", "cat_id=?", arrayOf(catId.toString()))
    }

    /** Keep only the last 3 months of data. */
    fun deleteOldData() {
        writableDatabase.delete("txns", "date < ?", arrayOf(dateStr(cutoffCal())))
    }

    private fun queryTxns(where: String, arg: String): List<Txn> {
        val out = mutableListOf<Txn>()
        readableDatabase.rawQuery(
            "SELECT t.id, c.name, c.icon, c.type, t.amount, t.note, t.date FROM txns t " +
                    "JOIN categories c ON c.id=t.cat_id WHERE $where ORDER BY t.date DESC, t.id DESC",
            arrayOf(arg)
        ).use { c ->
            while (c.moveToNext()) out.add(
                Txn(c.getLong(0), c.getString(1), c.getString(2) ?: "🏷️", c.getString(3),
                    c.getDouble(4), c.getString(5) ?: "", c.getString(6))
            )
        }
        return out
    }

    fun txnsForCategory(catId: Long) = queryTxns("t.cat_id=?", catId.toString())
    fun txnsForDate(date: String) = queryTxns("t.date=?", date)

    // ---------- totals ----------
    fun totalsByCategory(month: String): List<CatTotal> {
        val out = mutableListOf<CatTotal>()
        readableDatabase.rawQuery(
            "SELECT c.id, c.name, c.type, c.icon, IFNULL(SUM(t.amount),0), IFNULL(c.allowance,0) FROM categories c " +
                    "LEFT JOIN txns t ON t.cat_id=c.id AND t.date LIKE ? " +
                    "WHERE c.active=1 OR t.id IS NOT NULL GROUP BY c.id ORDER BY c.type, c.custom, c.id",
            arrayOf("$month-%")
        ).use { c ->
            while (c.moveToNext()) out.add(
                CatTotal(c.getLong(0), c.getString(1), c.getString(2), c.getString(3) ?: "🏷️", c.getDouble(4), c.getDouble(5))
            )
        }
        return out
    }

    fun monthTotal(type: String, month: String): Double {
        readableDatabase.rawQuery(
            "SELECT IFNULL(SUM(t.amount),0) FROM txns t JOIN categories c ON c.id=t.cat_id " +
                    "WHERE c.type=? AND t.date LIKE ?",
            arrayOf(type, "$month-%")
        ).use { c -> return if (c.moveToFirst()) c.getDouble(0) else 0.0 }
    }
}