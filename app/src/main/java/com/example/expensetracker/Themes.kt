package com.example.expensetracker

import android.content.Context

data class ThemeInfo(val name: String, val desc: String, val style: Int, val swatch: Int)

object Themes {
    val ALL = listOf(
        ThemeInfo("Midnight Blue", "Default  •  deep navy with electric blue", R.style.Theme_Expense, 0xFF2F80FF.toInt()),
        ThemeInfo("Teal Slate", "Teal and cool grey", R.style.Theme_Expense_Teal, 0xFF3FB8AF.toInt()),
        ThemeInfo("Black Gold", "Soft gold on black", R.style.Theme_Expense_Gold, 0xFFC9A24A.toInt()),
        ThemeInfo("Rose Velvet", "Shades of pink on plum black", R.style.Theme_Expense_Pink, 0xFFD9769B.toInt())
    )

    private fun p(c: Context) = c.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    fun index(c: Context): Int = p(c).getInt("theme", 0).coerceIn(0, ALL.size - 1)
    fun set(c: Context, i: Int) { p(c).edit().putInt("theme", i).apply() }
    fun styleRes(c: Context): Int = ALL[index(c)].style
}