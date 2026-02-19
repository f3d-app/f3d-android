package app.f3d.F3D.android.Utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class FileInteractionContract : ActivityResultContract<Void?, Uri?>() {
    override fun createIntent(context: Context, input: Void?): Intent {
        // Create an intent to open files with supported mime types
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("*/*")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, FileType.supportedMimeTypes)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        // Check the result code and parse the result if successful
        if (resultCode == Activity.RESULT_OK && intent != null) {
            return intent.data
        }
        return null
    }
}
