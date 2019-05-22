package com.dev.rakeshvasal.cameraopencvtest.CardRecognitionCode;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.dev.rakeshvasal.cameraopencvtest.R;

public class CardOutputActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_output);
        ImageView originalView = findViewById(R.id.originalBitmap);
        ImageView resultView = findViewById(R.id.resultBitmap);
        if (getIntent().hasExtra("originalBitmap")) {
            String originalBitmap = getIntent().getStringExtra("originalBitmap");
            Log.d("originalBitmap", originalBitmap);
           // originalView.setImageBitmap(originalBitmap);
        }
        if (getIntent().hasExtra("resultBitmap")) {
            String resultBitmap = getIntent().getStringExtra("resultBitmap");
            Log.d("resultBitmap", resultBitmap);
            //resultView.setImageBitmap(resultBitmap);
        }
    }
}
