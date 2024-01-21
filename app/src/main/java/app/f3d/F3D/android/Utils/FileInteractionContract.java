package app.f3d.F3D.android.Utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;

public class FileInteractionContract extends ActivityResultContract<Void, Uri> {

    @Override
    public Intent createIntent(Context context, Void input) {
        // Create an intent to open files with supported mime types
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, FileType.supportedMimeTypes);
        return intent;
    }

    @Override
    public Uri parseResult(int resultCode, Intent intent) {
        // Check the result code and parse the result if successful
        if (resultCode == android.app.Activity.RESULT_OK && intent != null) {
            return intent.getData();
        }
        return null;
    }
}

