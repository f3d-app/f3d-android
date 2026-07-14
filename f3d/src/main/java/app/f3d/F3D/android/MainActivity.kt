package app.f3d.F3D.android

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import app.f3d.F3D.android.Utils.FileInteractionContract
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private var mView: MainView? = null
    private var fileInteractionLauncher: ActivityResultLauncher<Void?>? = null
    private var optionsPanel: OptionsPanel? = null
    private var menuButton: ImageButton? = null

    private var edgeSizePx = 0
    private var edgeSwipeActive = false
    private lateinit var edgeSwipeDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainLayout = findViewById<ConstraintLayout>(R.id.mainLayout)

        val addButton = findViewById<FloatingActionButton>(R.id.addButton)
        menuButton = findViewById(R.id.menuButton)

        supportActionBar!!.hide()

        mView = MainView(this)

        edgeSizePx = (EDGE_SWIPE_DP * resources.displayMetrics.density).toInt()
        edgeSwipeDetector = GestureDetector(this, EdgeSwipeListener())

        menuButton!!.setOnClickListener { _: View? ->
            val panel = optionsPanel
            if (panel != null) {
                panel.dismiss()
            } else {
                openPanel()
            }
        }

        handleSelectedFileAppNotOpen()

        fileInteractionLauncher = registerForActivityResult(
            FileInteractionContract()
        ) { uri: Uri? -> this.handleSelectedFile(uri) }

        addButton.setOnClickListener { _: View? ->
            fileInteractionLauncher!!.launch(null)
        }

        mainLayout.addView(mView, 0)

        mainLayout.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            updateGestureExclusion(v)
        }
    }

    // Reserve a band on the right edge from the system back gesture so the app
    // receives the swipe-to-open-menu instead. Height is capped at 200dp by the system.
    private fun updateGestureExclusion(root: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val bandHalf = (GESTURE_BAND_DP * resources.displayMetrics.density / 2).toInt()
        val centerY = root.height / 2
        root.systemGestureExclusionRects = listOf(
            Rect(root.width - edgeSizePx, centerY - bandHalf, root.width, centerY + bandHalf)
        )
    }

    private fun openPanel() {
        if (optionsPanel != null) return
        optionsPanel = OptionsPanel(this, mView!!).apply {
            onDismiss = {
                optionsPanel = null
                menuButton?.setImageResource(R.drawable.ic_baseline_menu_24)
            }
            show()
        }
        menuButton?.setImageResource(R.drawable.ic_baseline_close_24)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            edgeSwipeActive = optionsPanel == null &&
                ev.x >= (window.decorView.width - edgeSizePx)
        }
        if (edgeSwipeActive) {
            edgeSwipeDetector.onTouchEvent(ev)
            if (ev.actionMasked == MotionEvent.ACTION_UP ||
                ev.actionMasked == MotionEvent.ACTION_CANCEL
            ) {
                edgeSwipeActive = false
            }
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    private inner class EdgeSwipeListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            if (e1 == null) return false
            val distanceX = e1.x - e2.x
            if (velocityX < 0 &&
                abs(velocityX) > abs(velocityY) &&
                distanceX > edgeSizePx
            ) {
                openPanel()
                return true
            }
            return false
        }
    }

    private fun handleSelectedFile(uri: Uri?) {
        mView!!.updateActiveUri(uri)
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

    companion object {
        private const val EDGE_SWIPE_DP = 20f
        private const val GESTURE_BAND_DP = 200f
    }
}
