package app.f3d.F3D.android

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import app.f3d.F3D.android.Utils.OptionSpec
import app.f3d.F3D.android.Utils.OptionWidget
import app.f3d.F3D.android.Utils.OptionsRegistry
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A Material bottom sheet listing a curated set of libf3d options.
 * The widget shown for each option is derived from its type and domain (resolved on the rendering
 * thread by [MainView.snapshotOptions]), so the registry only needs a name and a label.
 */
class OptionsPanel(baseContext: Context, private val view: MainView, sheet: View) :
    SheetPanel(sheet, sheet.findViewById<NestedScrollView>(R.id.optionsScroll)) {

    private val context: Context =
        ContextThemeWrapper(baseContext, R.style.Theme_F3D_OptionsPanel)

    private val touchSlop: Int = ViewConfiguration.get(baseContext).scaledTouchSlop

    private val container: LinearLayout = sheet.findViewById(R.id.optionsContainer)

    private var loaded = false

    fun refresh() {
        view.snapshotOptions(OptionsRegistry.v1) { widgets ->
            if (widgets.isNotEmpty()) {
                populate(widgets)
                loaded = true
            }
        }
    }

    override fun show() {
        if (loaded) {
            openSheet()
        } else {
            view.snapshotOptions(OptionsRegistry.v1) { widgets ->
                populate(widgets)
                loaded = widgets.isNotEmpty()
                openSheet()
            }
        }
    }

    private fun populate(widgets: List<OptionWidget>) {
        val root = container
        root.removeAllViews()
        root.addView(titleRow())

        if (widgets.isEmpty()) {
            root.addView(TextView(context).apply {
                text = context.getString(R.string.options_unavailable)
                setPadding(0, dp(16), 0, 0)
            })
        } else {
            for ((group, groupWidgets) in widgets.groupBy { it.spec.group }) {
                root.addView(header(group))
                val card = card()
                groupWidgets.forEach { card.addView(rowFor(it)) }
                root.addView(card)
            }
        }
    }

    private fun titleRow(): View {
        val title = TextView(context).apply {
            text = context.getString(R.string.options)
            textSize = 22f
            setTextColor(context.getColor(R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val reset = TextView(context).apply {
            text = context.getString(R.string.reset)
            textSize = 14f
            setTextColor(context.getColor(R.color.yellow))
            setPadding(dp(12), dp(6), dp(12), dp(6))
            isClickable = true
            val bg = android.util.TypedValue()
            context.theme.resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless, bg, true
            )
            setBackgroundResource(bg.resourceId)
            setOnClickListener {
                view.resetOptions(OptionsRegistry.v1) { widgets -> populate(widgets) }
            }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(title)
            addView(reset)
        }
    }

    private fun rowFor(widget: OptionWidget): View {
        val row = when (widget) {
            is OptionWidget.Toggle -> boolRow(widget)
            is OptionWidget.Enum -> enumRow(widget)
            is OptionWidget.Color -> colorRow(widget)
            is OptionWidget.Range -> rangeRow(widget)
        }
        // Dim optional options that are currently unset until the user gives them a value
        row.alpha = if (widget.isSet) 1f else INACTIVE_ALPHA
        return row
    }

    private fun boolRow(widget: OptionWidget.Toggle): View =
        MaterialSwitch(context).apply {
            text = widget.spec.label
            isChecked = widget.value
            layoutParams = rowParams()
            setOnCheckedChangeListener { _, checked ->
                alpha = 1f
                view.applyOption { it.setAsBool(widget.spec.name, checked) }
            }
        }

    private fun enumRow(widget: OptionWidget.Enum): View {
        val til = TextInputLayout(
            context, null,
            com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle
        ).apply {
            hint = widget.spec.label
            layoutParams = rowParams()
        }
        val dropdown = MaterialAutoCompleteTextView(til.context).apply {
            inputType = android.text.InputType.TYPE_NULL
            setSimpleItems(widget.values.toTypedArray())
            setText(widget.current, false)
            setOnItemClickListener { _, _, position, _ ->
                til.alpha = 1f
                view.applyOption { it.setAsStringRepresentation(widget.spec.name, widget.values[position]) }
            }
        }
        til.addView(dropdown)
        return til
    }

    private fun colorRow(widget: OptionWidget.Color): View {
        val currentRgb = widget.rgb.copyOf()
        val swatch = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            background = swatchDrawable(toAndroidColor(currentRgb))
        }
        val label = TextView(context).apply {
            text = widget.spec.label
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = rowParams()
            setPadding(0, dp(6), 0, dp(6))
            addView(label)
            addView(swatch)
        }
        row.setOnClickListener {
            showColorDialog(widget.spec, currentRgb.copyOf(), widget.isSet) { applied ->
                applied.copyInto(currentRgb)
                swatch.background = swatchDrawable(toAndroidColor(applied))
                row.alpha = 1f
            }
        }
        return row
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun rangeRow(widget: OptionWidget.Range): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = rowParams()
        }
        row.addView(TextView(context).apply {
            text = widget.spec.label
            textSize = 16f
        })
        row.addView(Slider(context).apply {
            valueFrom = widget.min.toFloat()
            valueTo = widget.max.toFloat()
            stepSize = widget.increment.toFloat()
            val steps = ((widget.current - widget.min) / widget.increment).roundToInt()
            value = (widget.min + steps * widget.increment)
                .coerceIn(widget.min, widget.max).toFloat()
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    row.alpha = 1f
                    view.applyOption { it.setAsDouble(widget.spec.name, value.toDouble()) }
                }
            }
            addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) = fadeSheet(DRAGGING_ALPHA)

                override fun onStopTrackingTouch(slider: Slider) = fadeSheet(1f)
            })
            // Claim the gesture on touch down so the side sheet does not steal the horizontal
            // drag as a dismiss before the slider starts tracking, then hand it back as soon as
            // the drag turns out to be vertical so the scroll view can scroll the panel.
            var downX = 0f
            var downY = 0f
            var directionSettled = false
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        downY = event.y
                        directionSettled = false
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_MOVE -> if (!directionSettled) {
                        val dx = abs(event.x - downX)
                        val dy = abs(event.y - downY)
                        if (dx > touchSlop || dy > touchSlop) {
                            directionSettled = true
                            if (dy > dx) {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                    }
                }
                false
            }
        })
        return row
    }

    /**
     * Simple RGB picker: three sliders and a live preview. The colour is applied to the scene as
     * it is dragged, and reverted on cancel, so the render itself acts as the preview.
     */
    private fun showColorDialog(
        spec: OptionSpec,
        rgb: DoubleArray,
        wasSet: Boolean,
        onApplied: (DoubleArray) -> Unit,
    ) {
        val initial = rgb.copyOf()
        var dialog: AlertDialog? = null

        // While a channel is being dragged, fade the picker itself so the render behind it stays
        // visible; the options panel is already hidden for the whole picker.
        fun fadeDialog(alpha: Float) {
            dialog?.window?.decorView?.animate()?.alpha(alpha)?.setDuration(FADE_DURATION_MS)?.start()
        }

        val preview = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
            background = roundedColor(toAndroidColor(rgb))
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
            addView(preview)
        }
        for (channel in 0..2) {
            content.addView(TextView(context).apply { text = charArrayOf('R', 'G', 'B')[channel].toString() })
            content.addView(SeekBar(context).apply {
                max = 255
                progress = (rgb[channel] * 255).toInt().coerceIn(0, 255)
                val amber = android.content.res.ColorStateList.valueOf(context.getColor(R.color.yellow))
                progressTintList = amber
                thumbTintList = amber
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) {
                        rgb[channel] = value / 255.0
                        preview.background = roundedColor(toAndroidColor(rgb))
                        if (fromUser) {
                            view.applyOption { it.setAsDoubleVector(spec.name, rgb) }
                        }
                    }

                    override fun onStartTrackingTouch(s: SeekBar?) = fadeDialog(DRAGGING_ALPHA)

                    override fun onStopTrackingTouch(s: SeekBar?) = fadeDialog(1f)
                })
            })
        }

        val revert = {
            view.applyOption {
                if (wasSet) {
                    it.setAsDoubleVector(spec.name, initial)
                } else {
                    it.removeValue(spec.name)
                }
            }
        }
        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(spec.label)
            .setView(content)
            .setPositiveButton("Apply") { _, _ -> onApplied(rgb) }
            .setNegativeButton("Cancel") { _, _ -> revert() }
            .setOnCancelListener { revert() }
            // Hide the options panel entirely while picking, so the whole viewport shows the colour
            // changing; the picker dialog stays up. Restored however the dialog is dismissed.
            .setOnDismissListener { fadeSheet(1f) }
            .create()
        dialog.apply {
            window?.setDimAmount(0f)
            setOnShowListener { fadeSheet(0f) }
            show()
        }
    }

    // --- helpers ---

    private fun toAndroidColor(rgb: DoubleArray): Int {
        fun c(i: Int) = (rgb.getOrElse(i) { 0.0 } * 255).toInt().coerceIn(0, 255)
        return Color.rgb(c(0), c(1), c(2))
    }

    private fun header(title: String): TextView = TextView(context).apply {
        text = title.uppercase()
        setPadding(dp(4), dp(20), 0, dp(8))
        setTextColor(context.getColor(R.color.yellow))
        textSize = 12f
        letterSpacing = 0.08f
    }

    private fun card(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = roundedColor(context.getColor(R.color.panel_card))
        setPadding(dp(16), dp(6), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun roundedColor(color: Int): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(16).toFloat()
        setColor(color)
    }

    private fun swatchDrawable(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(dp(1), Color.argb(60, 255, 255, 255))
    }

    private fun rowParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val INACTIVE_ALPHA = 0.4f
        const val DRAGGING_ALPHA = 0.15f
    }
}
