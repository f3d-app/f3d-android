package app.f3d.F3D.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    MainView mView;

    FloatingActionButton addButton;

    private final ActivityResultLauncher<String> getContentLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            result -> {
                if (result != null) {
                    // Handle the selected file URI
                    handleSelectedFile(result);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);

        ConstraintLayout mainLayout = findViewById(R.id.mainLayout);

        addButton = findViewById(R.id.addButton);

        addButton.setOnClickListener(view -> {
            openFilePicker();
        });

        mView = new MainView(this);

        mainLayout.addView(mView);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null) {
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                int code;
                StringBuilder sb = new StringBuilder();
                while((code = inputStream.read()) != -1) {
                    sb.append((char)code);
                }

                mView.openBuffer(sb.toString(), intent.getType());

                Log.e("ACTIVITY", "Content= " + sb);
            }
            catch(FileNotFoundException e)
            {
                Log.e("ACTIVITY", "No file " + uri + ": " + e);
            }
            catch(IOException e)
            {
                Log.e("ACTIVITY", "IO exception " + uri + ": " + e);
            }
        }
    }

    @Override protected void onPause()
    {
        super.onPause();
        this.mView.onPause();
    }

    @Override protected void onResume()
    {
        super.onResume();
        this.mView.onResume();
    }

    private void openFilePicker() {
        getContentLauncher.launch("*/*"); // Specify the MIME type if needed
    }

    private void handleSelectedFile(Uri uri) {
        String filePath = FileUtils.createTempFileFromUri(this,uri);
        mView.updateFilePath(filePath);
    }

}