package app.f3d.F3D.android

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import app.f3d.F3D.android.Utils.FileInteractionContract
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Objects

class MainActivity : AppCompatActivity() {
    private var mView: MainView? = null
    private var fileInteractionLauncher: ActivityResultLauncher<Void?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainLayout = findViewById<ConstraintLayout>(R.id.mainLayout)

        val addButton = findViewById<FloatingActionButton>(R.id.addButton)

        supportActionBar!!.hide()

        mView = MainView(this)

        handleSelectedFileAppNotOpen()

        fileInteractionLauncher = registerForActivityResult<Void?, Uri?>(
            FileInteractionContract(),
            ActivityResultCallback { uri: Uri? -> this.handleSelectedFile(uri) })

        addButton.setOnClickListener(View.OnClickListener { view: View? ->
            fileInteractionLauncher!!.launch(null)
        })

        mainLayout.addView(mView)
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

    override fun onPause() {
        super.onPause()
        mView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        mView!!.onResume()
    }
}
