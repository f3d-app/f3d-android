package app.f3d.F3D.android;

import android.app.ActionBar;
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
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private MainView mView;

    private final ActivityResultLauncher<String> getContentLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            result -> {
                if (result != null) {
                    handleSelectedFile(result);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConstraintLayout mainLayout = findViewById(R.id.mainLayout);

        FloatingActionButton addButton = findViewById(R.id.addButton);

        Objects.requireNonNull(getSupportActionBar()).hide();

        addButton.setOnClickListener(view -> {
            getContentLauncher.launch("*/*");
        });

        mView = new MainView(this);

        handleSelectedFileAppNotOpen();

        mainLayout.addView(mView);
    }


    @Override protected void onPause()
    {
        super.onPause();
        mView.onPause();
    }

    @Override protected void onResume()
    {
        super.onResume();
        mView.onResume();
    }

    private void handleSelectedFile(Uri uri) {
        String filePath = FileUtils.createTempFileFromUri(this,uri);
        mView.updateFilePath(filePath);
    }

    private void handleSelectedFileAppNotOpen(){
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            Uri uri = intent.getData();
            handleSelectedFile(uri);
        }
    }
}