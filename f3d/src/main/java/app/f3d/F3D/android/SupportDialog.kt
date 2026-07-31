package app.f3d.F3D.android

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SupportDialog(private val activity: AppCompatActivity) {

    private fun dp(value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()

    fun show() {
        val primaryColor = activity.getColor(R.color.yellow)
        val textColor = activity.getColor(R.color.white)
        val backgroundColor = activity.getColor(R.color.panel_surface)

        // Heart icon
        val icon = ImageView(activity).apply {
            setImageResource(R.drawable.ic_baseline_support_24)
            imageTintList = android.content.res.ColorStateList.valueOf(activity.getColor(R.color.red))
            layoutParams = LayoutParams(dp(56), dp(56)).apply {
                gravity = Gravity.CENTER
                bottomMargin = dp(16)
            }
        }

        // Title
        val title = TextView(activity).apply {
            text = context.getString(R.string.support_f3d)
            setTextColor(textColor)
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        // Description
        val desc = TextView(activity).apply {
            text = context.getString(R.string.support_f3d_description)
            setTextColor(ColorUtils.setAlphaComponent(textColor, 192))
            textSize = 15f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(24) }
        }

        // Donate button
        val donateBtn = TextView(activity).apply {
            text = context.getString(R.string.donate)
            setTextColor(activity.getColor(R.color.black))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(14), dp(24), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(50).toFloat()
                setColor(primaryColor)
            }
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }

        // Dismiss link
        val dismissBtn = TextView(activity).apply {
            text = context.getString(R.string.maybe_later)
            setTextColor(ColorUtils.setAlphaComponent(textColor, 128))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(backgroundColor)
            }
            setPadding(dp(28), dp(32), dp(28), dp(20))
            addView(icon)
            addView(title)
            addView(desc)
            addView(donateBtn)
            addView(dismissBtn)
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setBackground(GradientDrawable().apply { setColor(Color.TRANSPARENT) })
            .setView(content)
            .create()
        dialog.window?.setDimAmount(0.5f)

        donateBtn.setOnClickListener {
            dialog.dismiss()
            showDonateWebView()
        }
        dismissBtn.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showDonateWebView() {
        CustomTabsIntent.Builder()
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(activity.getColor(R.color.panel_surface))
                    .build()
            )
            .setShowTitle(false)
            .build()
            .launchUrl(activity, activity.getString(R.string.donate_url).toUri())
    }
}
