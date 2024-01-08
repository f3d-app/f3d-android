package app.f3d.F3D.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

    MainView mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mView = new MainView(this);

        this.setContentView(R.layout.activity_main);

        ConstraintLayout mainLayout = findViewById(R.id.mainLayout);
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

}