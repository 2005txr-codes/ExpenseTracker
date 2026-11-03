package com.example.expensetracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Donut chart with centre total and a legend, drawn with Canvas (no libraries). */
class PieChartView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private var data = listOf<Pair<String, Double>>()
    private var centerText = ""
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    fun setData(d: List<Pair<String, Double>>, center: String = "") {
        data = d
        centerText = center
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val colors = C.CHART
        val d = resources.displayMetrics.density
        val total = data.sumOf { it.second }
        val size = minOf(height.toFloat(), width / 2f) - 16 * d
        val stroke = size * 0.2f
        val left = 8 * d + stroke / 2
        rect.set(left, left, 8 * d + size - stroke / 2, 8 * d + size - stroke / 2)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.BUTT
        if (total <= 0) {
            paint.color = C.CARD2
            canvas.drawArc(rect, 0f, 360f, false, paint)
        } else {
            var start = -90f
            data.forEachIndexed { i, item ->
                paint.color = colors[i % colors.size]
                val sweep = (item.second / total * 360).toFloat()
                val gap = if (data.size > 1) 2f else 0f
                canvas.drawArc(rect, start, maxOf(sweep - gap, 0.5f), false, paint)
                start += sweep
            }
        }

        // centre text
        val cx = 8 * d + size / 2
        val cy = 8 * d + size / 2
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.color = C.TEXT
        paint.textSize = 15 * d
        paint.isFakeBoldText = true
        canvas.drawText(centerText, cx, cy, paint)
        paint.isFakeBoldText = false
        paint.color = C.MUTED
        paint.textSize = 11 * d
        canvas.drawText("spent", cx, cy + 16 * d, paint)

        // legend
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 12 * d
        var y = 28 * d
        val x = 8 * d + size + 20 * d
        data.forEachIndexed { i, item ->
            paint.color = colors[i % colors.size]
            canvas.drawCircle(x + 5 * d, y - 4 * d, 5 * d, paint)
            paint.color = C.TEXT
            val pct = if (total > 0) (item.second / total * 100).toInt() else 0
            canvas.drawText("${item.first} $pct%", x + 18 * d, y, paint)
            y += 22 * d
        }
    }
}