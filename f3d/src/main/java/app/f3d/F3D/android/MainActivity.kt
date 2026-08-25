package app.f3d.F3D.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import app.f3d.F3D.android.Utils.ConsoleLog
import app.f3d.F3D.android.Utils.FileInteractionContract
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.MaterialShapeDrawable

class MainActivity : AppCompatActivity() {
    private var mView: MainView? = null
    private var fileInteractionLauncher: ActivityResultLauncher<Void?>? = null
    private var optionsPanel: OptionsPanel? = null
    private var scenePanel: ScenePanel? = null
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
            onSlideOffset = { updateCradle() }
        }
        scenePanel = ScenePanel(this, mView!!, findViewById(R.id.sceneSheet)).apply {
            onSlideOffset = { updateCradle() }
        }
        keepSheetAboveBar()
        keepAnimControlsAboveFab()
        optionsPanel!!.refresh()

        findViewById<ImageButton>(R.id.optionsButton).setOnClickListener { _: View? ->
            scenePanel!!.dismiss()
            optionsPanel!!.apply { if (isOpen) dismiss() else show() }
        }

        findViewById<ImageButton>(R.id.sceneButton).setOnClickListener { _: View? ->
            optionsPanel!!.dismiss()
            scenePanel!!.apply { if (isOpen) dismiss() else show() }
        }

        findViewById<ImageButton>(R.id.consoleButton).setOnClickListener { _: View? ->
            openConsole()
        }

        findViewById<ImageButton>(R.id.supportButton).setOnClickListener { _: View? ->
            SupportDialog(this).show()
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
        mView!!.onSceneLoaded = { scenePanel?.refresh() }

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

    private fun updateCradle() {
        val bg = bottomAppBar?.background as? MaterialShapeDrawable ?: return
        val offset = maxOf(optionsPanel?.slideOffset ?: -1f, scenePanel?.slideOffset ?: -1f)
        bg.interpolation = (-offset).coerceIn(0f, 1f)
    }

    private fun openConsole() {
        if (consolePanel != null) return
        optionsPanel?.dismiss()
        scenePanel?.dismiss()
        setChromeVisible(false)
        consolePanel = ConsolePanel(this, mView!!).apply {
            onDismiss = {
                consolePanel = null
                setChromeVisible(true)
            }
            show()
        }
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

    override fun onDestroy() {
        mView?.destroyEngine()
        super.onDestroy()
    }
}
