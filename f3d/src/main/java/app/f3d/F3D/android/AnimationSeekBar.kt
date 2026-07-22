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
        val top = paddingTop.toFloat()
        val bottom = (height - paddingBottom).toFloat()
        for (fraction in keyFrameFractions) {
            val x = paddingLeft + fraction.coerceIn(0f, 1f) * trackWidth
            canvas.drawLine(x, top, x, bottom, tickPaint)
        }
    }
}
