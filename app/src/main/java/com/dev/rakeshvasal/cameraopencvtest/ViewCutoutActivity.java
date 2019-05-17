package com.dev.rakeshvasal.cameraopencvtest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.dev.rakeshvasal.cameraopencvtest.CardRecognitionCode.JavaCameraViewActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class ViewCutoutActivity extends AppCompatActivity {

    RelativeLayout mainLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_cutout);
        mainLayer = findViewById(R.id.mainLayer);
        mainLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ViewCutoutActivity.this, JavaCameraViewActivity.class);
                startActivity(intent);
            }
        });
        addHelpOverlay();
    }

    private void addHelpOverlay() {
        FocusView view = new FocusView(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        view.setLayoutParams(layoutParams);
        mainLayer.addView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("a", " -> OnResume");
        try {
            super.onResume();
            if (!OpenCVLoader.initDebug()) {
                Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
            } else {
                Log.d("OpenCV", "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
        Log.d("a", " <- OnResume");
    }


    @Override
    public void onPause() {
        super.onPause();
        /*if (CameraView != null)
            CameraView.disableView();*/
    }

    public void onDestroy() {
        super.onDestroy();
        /*if (CameraView != null)
            CameraView.disableView();*/
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    //mCamera = openFrontFacingCamera();
                    //Camera.Parameters parameters = mCamera.getParameters();
                    //List<Camera.Size> resList = mCamera.getParameters().getSupportedPictureSizes();
                    int listNum = 1;// 0 is the maximum resolution
                    //width = resList.get(listNum).width;
                    //height = resList.get(listNum).height;
                    //myCameraView.setMaxFrameSize(width, height);
                    //myCameraView.enableView();
                    //CameraView.enableView();
                    //mOpenCvCameraView.setMaxFrameSize(width, height);


                    //compare();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
}
