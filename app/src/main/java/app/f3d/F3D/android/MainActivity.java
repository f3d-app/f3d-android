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

import java.util.Arrays;
import java.util.Objects;

import app.f3d.F3D.android.Utils.FileInteractionContract;
import app.f3d.F3D.android.Utils.FileType;
import app.f3d.F3D.android.Utils.FileUtils;

public class MainActivity extends AppCompatActivity {

    private MainView mView;
    private ActivityResultLauncher<Void> fileInteractionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConstraintLayout mainLayout = findViewById(R.id.mainLayout);

        FloatingActionButton addButton = findViewById(R.id.addButton);

        Objects.requireNonNull(getSupportActionBar()).hide();

        mView = new MainView(this);

        handleSelectedFileAppNotOpen();

        fileInteractionLauncher = registerForActivityResult(new FileInteractionContract(), this::handleSelectedFile);

        addButton.setOnClickListener(view -> {
            fileInteractionLauncher.launch(null);
        });

        mainLayout.addView(mView);
    }

    private void handleSelectedFile(Uri uri) {
        String filePath = FileUtils.createTempFileFromUri(this,uri);
        boolean useGeometry = FileType.checkFileTypeMethod(FileUtils.getFileExtension());
        mView.updateFilePath(filePath,useGeometry);
    }

    private void handleSelectedFileAppNotOpen(){
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            Uri uri = intent.getData();
            handleSelectedFile(uri);
        }
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
}