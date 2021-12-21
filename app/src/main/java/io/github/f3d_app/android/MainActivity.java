package io.github.f3d_app.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.app.Activity;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import io.github.f3d_app.android.R;

public class MainActivity extends Activity {

    MainView mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mView = new MainView(this);

        this.setContentView(R.layout.activity_main);

        ConstraintLayout mainLayout = findViewById(R.id.mainLayout);
        mainLayout.addView(mView);

        FloatingActionButton previousButton = findViewById(R.id.previousButton);
        previousButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mView.loadPrevious();
            }
        });

        FloatingActionButton nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mView.loadNext();
            }
        });
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                int code;
                String str = "";
                while((code = inputStream.read()) != -1) {
                    str += (char)code;
                }
                Log.e("ACTIVITY", "Content= " + str);
            }
            catch(FileNotFoundException e)
            {

            }
            catch(IOException e)
            {

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