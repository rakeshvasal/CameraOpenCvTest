package com.dev.rakeshvasal.cameraopencvtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.vision.CameraSource;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class JavaCameraViewActivity extends AppCompatActivity implements CameraSource.PictureCallback {


    CameraLab CameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_camera_view);
        CameraView = (CameraLab) findViewById(R.id.surface_view);
        //CameraView = new CameraLab(this,1);
        Button bt = findViewById(R.id.btn);

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String currentDateandTime = sdf.format(new Date());
                String fileName = "Image_" + currentDateandTime + ".jpg";
                CameraView.takePicture(fileName);
            }
        });
    }

    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
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
                    CameraView.enableView();
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
        if (CameraView != null)
            CameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (CameraView != null)
            CameraView.disableView();
    }

    @Override
    public void onPictureTaken(byte[] bytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Log.d("bitmap", "" + bitmap.getWidth());

    }

}
