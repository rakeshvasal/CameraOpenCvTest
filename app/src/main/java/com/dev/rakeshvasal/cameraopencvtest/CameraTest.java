package com.dev.rakeshvasal.cameraopencvtest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CameraTest extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1001;
    private static final int IMAGE_PREVIEW = 1002;
    CheckBox flashcheck;
    Camera.Parameters params;
    public static int CAM1 = 1;
    FrameLayout preview;
    int cameraId = -1;
    public static String TAG = "CameraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCamera = openFrontFacingCamera();
        setContentView(R.layout.activity_camera_test);
        Button captureButton = (Button) findViewById(R.id.button_capture);
        preview = (FrameLayout) findViewById(R.id.camera_preview);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            mCamera.takePicture(null, null, mPicture);
                        }
                    }
                });

            }
        });

        Button flash_switch = findViewById(R.id.flash_switch);
        flash_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setParameters();
            }
        });

    }


    private void setParameters() {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            Camera.Size size = sizes.get(0);
            Camera.Size optionalSize = getOptimalPreviewSize(sizes, size.width, size.height);
            params.setPreviewSize(optionalSize.width, optionalSize.height);
            params.setRotation(90);
            params.setPictureSize(size.width, size.height);
            params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            //setCameraDisplayOrientation(CameraActivity.this,cameraId,mCamera);
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            params.setExposureCompensation(0);
            params.setPictureFormat(ImageFormat.JPEG);
            params.setJpegQuality(100);
            mCamera.setParameters(params);
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkCameraPermissons() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        int camera = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
        int write_storage = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_storage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (camera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.CAMERA);
        }
        if (write_storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (read_storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);

        }
    }


    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    /**
     * Helper method to access the camera returns null if it cannot get the
     * camera or does not exist
     *
     * @return
     */
    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
            camera = Camera.open();

        } catch (Exception e) {
            e.printStackTrace();
            releaseCameraAndPreview();
            // cannot get camera or does not exist
        }
        return camera;
    }

    private void releaseCameraAndPreview() {

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        //setValues();
    }

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);

                fos.write(data);
                fos.close();

                Intent intent = new Intent(CameraTest.this, ImagePreviewActivity.class);

                String image_path = pictureFile.getPath();

                intent.putExtra("ImgURL", image_path);
                intent.putExtra("CamType", CAM1);
                //releaseCameraAndPreview();

                startActivityForResult(intent, IMAGE_PREVIEW);
                //finish();

            } catch (Exception e) {

                e.printStackTrace();

                releaseCameraAndPreview();
                finish();

            }


        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PREVIEW) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "OnActivityResult Call hua", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory() + "/ImageFolder/");
        //File mediaStorageDir = new File("");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + timeStamp + ".jpg");

        return mediaFile;
    }


    public Camera openFrontFacingCamera() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            Log.d(TAG, "Camera Info: " + cameraInfo.facing);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    return Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return null;
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, " -> OnResume");
        try {
            super.onResume();
            if (!OpenCVLoader.initDebug()) {
                Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
            } else {
                Log.d("OpenCV", "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
            mCamera = openFrontFacingCamera();
            if (mCamera != null) {
                mCameraPreview = new CameraPreview(this, mCamera);
                preview.addView(mCameraPreview);
                //setContentView(mCameraPreview);
            } else {
                Log.d(TAG, " = Camera == NULL");
            }
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
        Log.d(TAG, " <- OnResume");
    }

    @Override
    protected void onPause() {
        Log.d(TAG, " -> onPause");
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        super.onPause();
        Log.d(TAG, " <- onPause");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
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
