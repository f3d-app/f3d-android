package app.f3d.F3D.android

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import app.f3d.F3D.android.Utils.ConsoleLog
import app.f3d.F3D.android.Utils.FileInteractionContract
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.MaterialShapeDrawable

class MainActivity : AppCompatActivity() {
    private var mView: MainView? = null
    private var fileInteractionLauncher: ActivityResultLauncher<Void?>? = null
    private var optionsPanel: OptionsPanel? = null
    private var consolePanel: ConsolePanel? = null
    private var animationController: AnimationController? = null
    private var optionsSheet: View? = null
    private var addButton: FloatingActionButton? = null
    private var bottomAppBar: BottomAppBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainLayout = findViewById<CoordinatorLayout>(R.id.mainLayout)

        addButton = findViewById(R.id.addButton)
        bottomAppBar = findViewById(R.id.bottomAppBar)

        supportActionBar!!.hide()

        ConsoleLog.install()

        mView = MainView(this)

        optionsSheet = findViewById(R.id.optionsSheet)
        optionsPanel = OptionsPanel(this, mView!!, optionsSheet!!).apply {
            onSlideOffset = { offset -> setCradleFromSlide(offset) }
        }
        keepSheetAboveBar()
        keepAnimControlsAboveFab()
        optionsPanel!!.refresh()

        findViewById<ImageButton>(R.id.optionsButton).setOnClickListener { _: View? ->
            optionsPanel!!.apply { if (isOpen) dismiss() else show() }
        }

        findViewById<ImageButton>(R.id.consoleButton).setOnClickListener { _: View? ->
            openConsole()
        }

        findViewById<ImageButton>(R.id.supportButton).setOnClickListener { _: View? ->
            showSupportDialog()
        }

        handleSelectedFileAppNotOpen()

        fileInteractionLauncher = registerForActivityResult(
            FileInteractionContract()
        ) { uri: Uri? -> this.handleSelectedFile(uri) }

        addButton!!.setOnClickListener { _: View? ->
            fileInteractionLauncher!!.launch(null)
        }

        mainLayout.addView(
            mView,
            0,
            CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        animationController = AnimationController(mView!!, mainLayout)
        mView!!.onViewportTouch = { animationController?.onViewportInteraction() }

        applyWindowInsets(mainLayout)
    }

    private fun applyWindowInsets(mainLayout: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val infoBar = findViewById<View>(R.id.animInfoBar)
        val infoBaseTop = (infoBar.layoutParams as ViewGroup.MarginLayoutParams).topMargin

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { _, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top

            (infoBar.layoutParams as ViewGroup.MarginLayoutParams).let {
                if (it.topMargin != infoBaseTop + top) {
                    it.topMargin = infoBaseTop + top
                    infoBar.requestLayout()
                }
            }
            insets
        }
    }

    private fun keepAnimControlsAboveFab() {
        val controls = findViewById<View>(R.id.animControlBar)
        val gap = (-15 * resources.displayMetrics.density).toInt()
        addButton?.addOnLayoutChangeListener { fab, _, top, _, _, _, _, _, _ ->
            val margin = (fab.parent as View).height - top + gap
            val params = controls.layoutParams as ViewGroup.MarginLayoutParams
            if (params.bottomMargin != margin) {
                params.bottomMargin = margin
                controls.post { controls.requestLayout() }
            }
        }
    }

    private fun keepSheetAboveBar() {
        val host = findViewById<View>(R.id.optionsSheetHost)
        host.outlineProvider = ViewOutlineProvider.BOUNDS
        host.clipToOutline = true
        bottomAppBar?.addOnLayoutChangeListener { bar, _, top, _, _, _, _, _, _ ->
            val inset = (bar.parent as View).height - top
            val params = host.layoutParams as ViewGroup.MarginLayoutParams
            if (params.bottomMargin != inset) {
                params.bottomMargin = inset
                host.post { host.requestLayout() }
            }
        }
    }

    private fun setCradleFromSlide(slideOffset: Float) {
        val bg = bottomAppBar?.background as? MaterialShapeDrawable ?: return
        bg.interpolation = (-slideOffset).coerceIn(0f, 1f)
    }

    private fun openConsole() {
        if (consolePanel != null) return
        optionsPanel?.dismiss()
        setChromeVisible(false)
        consolePanel = ConsolePanel(this, mView!!).apply {
            onDismiss = {
                consolePanel = null
                setChromeVisible(true)
            }
            show()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun showSupportDialog() {
        val primaryColor = getColor(R.color.yellow)
        val textColor = getColor(R.color.white)
        val backgroundColor = getColor(R.color.panel_surface)

        // Heart icon
        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_baseline_support_24)
            imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.red))
            layoutParams = LayoutParams(dp(56), dp(56)).apply {
                gravity = Gravity.CENTER
                bottomMargin = dp(16)
            }
        }

        // Title
        val title = TextView(this).apply {
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
        val desc = TextView(this).apply {
            text = context.getString(R.string.support_f3d_description)
            setTextColor(ColorUtils.setAlphaComponent(textColor, 192))
            textSize = 15f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(24) }
        }

        // Donate button
        val donateBtn = TextView(this).apply {
            text = context.getString(R.string.donate)
            setTextColor(getColor(R.color.black))
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
        val dismissBtn = TextView(this).apply {
            text = context.getString(R.string.maybe_later)
            setTextColor(ColorUtils.setAlphaComponent(textColor, 128))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
        }

        val content = LinearLayout(this).apply {
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

        val dialog = MaterialAlertDialogBuilder(this)
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
                    .setToolbarColor(getColor(R.color.panel_surface))
                    .build()
            )
            .setShowTitle(false)
            .build()
            .launchUrl(this, "https://donate.stripe.com/4gM00j7tO1w4eAD5J0cs800".toUri())
    }

    private fun setChromeVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        bottomAppBar?.visibility = visibility
        addButton?.visibility = visibility
        animationController?.setHiddenForChrome(!visible)
    }

    private fun handleSelectedFile(uri: Uri?) {
        mView!!.updateActiveUri(uri)
        optionsPanel?.refresh()
    }

    private fun handleSelectedFileAppNotOpen() {
        val intent = getIntent()
        if (intent != null && intent.data != null) {
            val uri = intent.data
            handleSelectedFile(uri)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data != null) {
            handleSelectedFile(intent.data)
        }
    }

    override fun onPause() {
        super.onPause()
        mView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        mView!!.onResume()
    }
}
