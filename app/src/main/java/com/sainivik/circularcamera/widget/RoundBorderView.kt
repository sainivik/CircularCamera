package com.sainivik.circularcamera.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class RoundBorderView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private var paint: Paint? = null
    private var radius = 0
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (paint == null) {
            paint = Paint()
            paint!!.style = Paint.Style.STROKE
            paint!!.isAntiAlias = true
            val sweepGradient = SweepGradient(
                width.toFloat() / 2,
                height.toFloat() / 2,
                intArrayOf(
                    Color.GREEN,
                    Color.CYAN,
                    Color.BLUE,
                    Color.CYAN,
                    Color.GREEN
                ),
                null
            )
            paint!!.shader = sweepGradient
        }
        drawBorder(canvas, 6)
    }

    private fun drawBorder(canvas: Canvas?, rectThickness: Int) {
        if (canvas == null) {
            return
        }
        paint!!.strokeWidth = rectThickness.toFloat()
        val drawPath = Path()
        drawPath.addRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            radius.toFloat(),
            radius.toFloat(),
            Path.Direction.CW
        )
        canvas.drawPath(drawPath, paint!!)
    }

    fun turnRound() {
        invalidate()
    }

    fun setRadius(radius: Int) {
        this.radius = radius
    }
}