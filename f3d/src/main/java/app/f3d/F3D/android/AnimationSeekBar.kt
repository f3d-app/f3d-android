package app.f3d.F3D.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar

class AnimationSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.seekBarStyle,
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    private var keyFrameFractions = FloatArray(0)

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.white)
        alpha = 140
        strokeWidth = context.resources.displayMetrics.density // 1dp
    }

    fun setKeyFrameFractions(fractions: FloatArray) {
        keyFrameFractions = fractions
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (keyFrameFractions.isEmpty()) return

        val trackWidth = width - paddingLeft - paddingRight
        val centerY = height / 2f
        val halfHeight = 0.6f * (thumb?.intrinsicHeight ?: height) / 2f
        val top = centerY - halfHeight
        val bottom = centerY + halfHeight
        for (fraction in keyFrameFractions) {
            val x = paddingLeft + fraction.coerceIn(0f, 1f) * trackWidth
            canvas.drawLine(x, top, x, bottom, tickPaint)
        }
    }
}
