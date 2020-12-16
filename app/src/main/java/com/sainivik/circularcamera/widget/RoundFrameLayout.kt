package com.sainivik.circularcamera.widget

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout

class RoundFrameLayout(
    context: Context?,
    attrs: AttributeSet?
) : FrameLayout(context!!, attrs) {
    var radius = 0

    constructor(context: Context) : this(context, null) {}

    fun turnRound() {
        invalidateOutline()
    }

    init {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val rect =
                    Rect(0, 0, view.measuredWidth, view.measuredHeight)
                outline.setRoundRect(rect, radius.toFloat())
            }
        }
        clipToOutline = true
    }
}