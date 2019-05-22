package com.dev.rakeshvasal.cameraopencvtest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

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
                // mCamera.takePicture(null, null, mPicture);
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
        setParameters();
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
           // params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            //setCameraDisplayOrientation(CameraActivity.this,cameraId,mCamera);
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            params.setExposureCompensation(0);
            params.setPictureFormat(ImageFormat.JPEG);
            //mCamera.setPreviewCallback(previewCallback);
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

    /*Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.d("Frames", "" + data.length);
            byte[] bitmapdata = data; // let this be your byte array
            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(bitmapdata);
            Bitmap bitmap = BitmapFactory.decodeStream(arrayInputStream);
            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);
            //new AsyncProcess(mat).execute();
        }
    };*/

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

                Intent intent = new Intent(CameraTest.this, ImageProcessingActivity.class);

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

    public class AsyncProcess extends AsyncTask<String, String, String> {

        Mat finalMat = new Mat();
        Mat originalMat = new Mat();

        public AsyncProcess(Mat original) {
            this.originalMat = original;
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                findRectangle(originalMat);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private void findRectangle(Mat src) throws Exception {
            Mat image_output = new Mat();
            Mat grayCon = new Mat();

            Imgproc.cvtColor(originalMat, grayCon, Imgproc.COLOR_BGR2GRAY);

            Mat blurred = src.clone();
            Imgproc.medianBlur(src, blurred, 9);

            Mat gray0 = new Mat(blurred.size(), CvType.CV_8U),
                    gray = new Mat();

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            List<Mat> blurredChannel = new ArrayList<Mat>();
            blurredChannel.add(blurred);
            List<Mat> gray0Channel = new ArrayList<Mat>();
            gray0Channel.add(gray0);

            MatOfPoint2f approxCurve;

            double maxArea = 0;
            int maxId = -1;

            for (int c = 0; c < 3; c++) {
                int ch[] = {c, 0};
                Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

                int thresholdLevel = 1;
                for (int t = 0; t < thresholdLevel; t++) {
                    if (t == 0) {
                        Imgproc.Canny(gray0, gray, 20, 40, 3, true); // true ?
                        Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                        // ?
                    } else {
                        Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                                Imgproc.THRESH_BINARY,
                                (originalMat.width() + originalMat.height()) / 200, t);
                    }

                    Imgproc.findContours(gray, contours, new Mat(),
                            Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                    for (MatOfPoint contour : contours) {
                        MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                        double area = Imgproc.contourArea(contour);
                        approxCurve = new MatOfPoint2f();
                        Imgproc.approxPolyDP(temp, approxCurve,
                                Imgproc.arcLength(temp, true) * 0.02, true);

                        if (approxCurve.total() == 4 && area >= maxArea) {
                            double maxCosine = 0;

                            List<Point> curves = approxCurve.toList();
                            for (int j = 2; j < 5; j++) {

                                double cosine = Math.abs(angle(curves.get(j % 4),
                                        curves.get(j - 2), curves.get(j - 1)));
                                maxCosine = Math.max(maxCosine, cosine);
                            }

                            if (maxCosine < 0.3) {
                                maxArea = area;
                                maxId = contours.indexOf(contour);
                            }
                        }
                    }
                }
            }


            if (maxId >= 0) {
                Imgproc.drawContours(src, contours, maxId, new Scalar(255, 0, 0,
                        .8), 8);
            }
        }


        @Override
        protected void onPostExecute(String s) {
            showBitmap(finalMat, null);
            super.onPostExecute(s);
        }
    }

    private double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)
                / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
                + 1e-10);
    }

    private void showBitmap(Mat mat, ImageView imageView) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        //imageView.setImageBitmap(bitmap);
    }

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
            setParameters();
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
