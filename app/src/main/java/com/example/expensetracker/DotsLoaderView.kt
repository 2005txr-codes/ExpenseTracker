package com.example.expensetracker

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/** Six coloured dots that ride a smooth wave, with a soft glow. */
class DotsLoaderView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val colors = intArrayOf(
        0xFFFFD93D.toInt(), 0xFFFF8A3D.toInt(), 0xFFFF4D6D.toInt(),
        0xFF9B6BFF.toInt(), 0xFF3DD9F5.toInt(), 0xFF3DDC84.toInt()
    )
    private var phase = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1600
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val r = 7 * d
        val gap = 26 * d
        val startX = (width - (colors.size - 1) * gap) / 2f
        val cy = height / 2f
        val amp = 14 * d
        for (i in colors.indices) {
            val ph = phase * 2.0 * Math.PI - i * 0.7
            val s = Math.sin(ph).toFloat()
            val x = startX + i * gap
            val y = cy + s * amp
            val scale = 0.85f + 0.3f * (s + 1f) / 2f
            paint.color = colors[i]
            paint.alpha = 50
            canvas.drawCircle(x, y, r * scale * 1.9f, paint)   // glow
            paint.color = colors[i]
            canvas.drawCircle(x, y, r * scale, paint)           // dot
        }
    }
}