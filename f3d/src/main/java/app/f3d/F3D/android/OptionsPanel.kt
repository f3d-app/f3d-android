package app.f3d.F3D.android

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import app.f3d.F3D.android.Utils.OptionSpec
import app.f3d.F3D.android.Utils.OptionWidget
import app.f3d.F3D.android.Utils.OptionsRegistry
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.sidesheet.SideSheetDialog
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout

/**
 * A Material side sheet listing a curated set of libf3d options.
 * The widget shown for each option is derived from its type and domain (resolved on the rendering
 * thread by [MainView.snapshotOptions]), so the registry only needs a name and a label.
 */
class OptionsPanel(baseContext: Context, private val view: MainView) {

    private val context: Context =
        ContextThemeWrapper(baseContext, R.style.Theme_F3D_OptionsPanel)

    /** Invoked when the panel is dismissed, whether by [dismiss] or a swipe. */
    var onDismiss: (() -> Unit)? = null
    private var dialog: SideSheetDialog? = null
    private var container: LinearLayout? = null

    fun show() {
        view.snapshotOptions(OptionsRegistry.v1) { widgets -> buildAndShow(widgets) }
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    private fun buildAndShow(widgets: List<OptionWidget>) {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(24))
        }
        container = root
        populate(widgets)

        dialog = SideSheetDialog(context).apply {
            setContentView(ScrollView(context).apply { addView(root) })
            setOnDismissListener { onDismiss?.invoke() }
            show()
        }
    }

    private fun populate(widgets: List<OptionWidget>) {
        val root = container ?: return
        root.removeAllViews()
        root.addView(titleRow())

        if (widgets.isEmpty()) {
            root.addView(TextView(context).apply {
                text = "Options unavailable"
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
            text = "Options"
            textSize = 22f
            setTextColor(context.getColor(R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val reset = TextView(context).apply {
            text = "Reset"
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
        val swatch = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            background = swatchDrawable(toAndroidColor(widget.rgb))
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
            showColorDialog(widget.spec, widget.rgb.copyOf()) {
                swatch.background = swatchDrawable(toAndroidColor(it))
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
            val steps = Math.round((widget.current - widget.min) / widget.increment)
            value = (widget.min + steps * widget.increment)
                .coerceIn(widget.min, widget.max).toFloat()
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    row.alpha = 1f
                    view.applyOption { it.setAsDouble(widget.spec.name, value.toDouble()) }
                }
            }
            // Claim the gesture on touch down so the side sheet (and scroll view) don't steal
            // the horizontal drag as a dismiss/scroll gesture before the slider starts tracking
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                false
            }
        })
        return row
    }

    /** Simple RGB picker: three sliders and a live preview. */
    private fun showColorDialog(spec: OptionSpec, rgb: DoubleArray, onApplied: (DoubleArray) -> Unit) {
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
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            })
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(spec.label)
            .setView(content)
            .setPositiveButton("Apply") { _, _ ->
                view.applyOption { it.setAsDoubleVector(spec.name, rgb) }
                onApplied(rgb)
            }
            .setNegativeButton("Cancel", null)
            .show()
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
    }
}
