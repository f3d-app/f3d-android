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
import app.f3d.F3D.android.Utils.ConsoleLog
import app.f3d.F3D.android.Utils.FileInteractionContract
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.MaterialShapeDrawable

class MainActivity : AppCompatActivity() {
    private var mView: MainView? = null
    private var fileInteractionLauncher: ActivityResultLauncher<Void?>? = null
    private var optionsPanel: OptionsPanel? = null
    private var consolePanel: ConsolePanel? = null
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
        optionsPanel!!.refresh()

        findViewById<ImageButton>(R.id.optionsButton).setOnClickListener { _: View? ->
            optionsPanel!!.apply { if (isOpen) dismiss() else show() }
        }

        findViewById<ImageButton>(R.id.consoleButton).setOnClickListener { _: View? ->
            openConsole()
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

    private fun setChromeVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        bottomAppBar?.visibility = visibility
        addButton?.visibility = visibility
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
