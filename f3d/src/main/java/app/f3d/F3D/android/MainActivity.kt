package app.f3d.F3D.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import app.f3d.F3D.android.Utils.FileInteractionContract
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    private var mView: MainView? = null
    private var fileInteractionLauncher: ActivityResultLauncher<Void?>? = null
    private var optionsPanel: OptionsPanel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainLayout = findViewById<ConstraintLayout>(R.id.mainLayout)

        val addButton = findViewById<FloatingActionButton>(R.id.addButton)
        val menuButton = findViewById<ImageButton>(R.id.menuButton)

        supportActionBar!!.hide()

        mView = MainView(this)

        menuButton.setOnClickListener { _: View? ->
            val panel = optionsPanel
            if (panel != null) {
                panel.dismiss()
            } else {
                optionsPanel = OptionsPanel(this, mView!!).apply {
                    onDismiss = {
                        optionsPanel = null
                        menuButton.setImageResource(R.drawable.ic_baseline_menu_24)
                    }
                    show()
                }
                menuButton.setImageResource(R.drawable.ic_baseline_close_24)
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
}
