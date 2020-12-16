package com.sainivik.circularcamera.widget

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.util.AttributeSet
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider

 class RoundTextureView(
    context: Context?,
    attrs: AttributeSet?
) : TextureView(context!!, attrs) {
     var radius: Int=0

    fun turnRound() {
        invalidateOutline()
    }

    companion object {
        private const val TAG = "CustomTextureView"
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