package com.dev.rakeshvasal.cameraopencvtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dev.rakeshvasal.cameraopencvtest.CardRecognitionCode.CameraLab;
import com.google.android.gms.vision.CameraSource;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class OpenCvCameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2,CameraSource.PictureCallback {

    public static String TAG = "CameraActivity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private JavaCameraView mOpenCvJavaCameraView;
    private MyCameraView myCameraView;
    private CameraLab CameraView;
    private int w, h;
    TextView tvName;
    Scalar RED = new Scalar(255, 0, 0);
    Scalar GREEN = new Scalar(0, 255, 0);
    FeatureDetector detector;
    DescriptorExtractor descriptor;
    DescriptorMatcher matcher;
    Mat descriptors2, descriptors1;
    Mat img1;
    MatOfKeyPoint keypoints1, keypoints2;
    private Mat mGrey, mRgba;
    private Rect finalrect;
    private static double areaThreshold = 0.025; //threshold for the area size of an object
    private static Scalar CONTOUR_COLOR = null;
    private Camera mCamera;
    int width;
    int height;

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
                    mCamera = openFrontFacingCamera();
                    Camera.Parameters parameters = mCamera.getParameters();
                    List<Camera.Size> resList = mCamera.getParameters().getSupportedPictureSizes();
                    int listNum = 1;// 0 is the maximum resolution
                    width = resList.get(listNum).width;
                    height = resList.get(listNum).height;
                    myCameraView.setMaxFrameSize(width, height);
                    myCameraView.enableView();
                    //mOpenCvCameraView.enableView();
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_cv_camera);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.surface_view);

        //myCameraView = (MyCameraView) findViewById(R.id.surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        //myCameraView.setCvCameraViewListener(this);
        tvName = (TextView) findViewById(R.id.text1);

        Button bt = findViewById(R.id.btn);
      /*  mCamera = openFrontFacingCamera();
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> resList = mCamera.getParameters().getSupportedPictureSizes();
        int listNum = 1;// 0 is the maximum resolution
        width = resList.get(listNum).width;
        height = resList.get(listNum).height;*/

        //mOpenCvCameraView.setMaxFrameSize(width, height);
        // myCameraView.setMaxFrameSize(width, height);
        //params.setPictureSize(width, height);
        //mCamera.setParameters(params);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //mOpenCvJavaCameraView.
            }
        });
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
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
        Log.d(TAG, " <- OnResume");
    }


    @Override
    public void onPause() {
        super.onPause();
        /*if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();*/
    }

    public void onDestroy() {
        super.onDestroy();
       /* if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();*/
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        w = width;
        h = height;
        mGrey = new Mat(width, height, CvType.CV_8UC1);
        mRgba = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(MyCameraView.CvCameraViewFrame inputFrame)  {
        //return nu
        //detectObject();
        System.gc();
        mRgba = inputFrame.rgba();
        try {
            detectRect(mRgba);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //new AsyncProcess(mRgba).execute();
        return inputFrame.rgba();
    }

    @Override
    public void onPictureTaken(byte[] bytes) {

    }

    private void detectRect(Mat src) throws Exception {
        Mat image_output = new Mat();
        Mat grayCon = new Mat();

        Imgproc.cvtColor(mRgba, grayCon, Imgproc.COLOR_BGR2GRAY);


        Mat blurred = grayCon.clone();
        Imgproc.medianBlur(mRgba, blurred, 9);


        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

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
                            (mRgba.width() + mRgba.height()) / 200, t);
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

            //if (maxArea > 30000 && maxArea < 40000) {
            //Log.d("Area Of Contour", ":" + maxArea);
            Log.d("Area Of Contour", ":" + maxArea);
            Imgproc.drawContours(mRgba, contours, maxId, new Scalar(255, 0, 0,
                    .8), 10);
            //Rect rect = Imgproc.boundingRect(contours.get(maxId));
            //finalMat = originalMat.submat(rect);
            //Core.flip(image_output.t(), image_output, 1);
            //sendInfo(mat[0],rect);
            //return image_output;
            Log.d("TAG", "Max Area Found");
            //}

        }

        mRgba.release();
        gray.release();
        gray0.release();

    }

    public class AsyncProcess extends AsyncTask<String, String, String> {

        Mat finalMat = new Mat();
        Mat originalMat = new Mat();

        public AsyncProcess(Mat original) {
            this.originalMat = original;
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                //findRectangle(originalMat);
                detectRect(originalMat);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private void findRectangle(Mat src) throws Exception {
            Mat blurred = src.clone();
            Imgproc.medianBlur(src, blurred, 9);

            Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

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
                        Imgproc.Canny(gray0, gray, 10, 20, 3, true); // true ?
                        Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                        // ?
                    } else {
                        Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                                Imgproc.THRESH_BINARY,
                                (src.width() + src.height()) / 200, t);
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
            finalMat = src;
        }

        private void detectRect(Mat src) throws Exception {
            Mat image_output = new Mat();
            Mat grayCon = new Mat();

            Imgproc.cvtColor(originalMat, grayCon, Imgproc.COLOR_BGR2GRAY);


            Mat blurred = grayCon.clone();
            Imgproc.medianBlur(originalMat, blurred, 9);


            Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

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

                //if (maxArea > 30000 && maxArea < 40000) {
                    //Log.d("Area Of Contour", ":" + maxArea);
                    Log.d("Area Of Contour", ":" + maxArea);
                Imgproc.drawContours(originalMat, contours, maxId, new Scalar(255, 0, 0,
                        .8), 10);
                    //Rect rect = Imgproc.boundingRect(contours.get(maxId));
                    //finalMat = originalMat.submat(rect);
                    //Core.flip(image_output.t(), image_output, 1);
                    //sendInfo(mat[0],rect);
                    //return image_output;
                    Log.d("TAG", "Max Area Found");
                //}

            }

            originalMat.release();
            gray.release();
            gray0.release();

        }

        @Override
        protected void onPostExecute(String s) {

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
        imageView.setImageBitmap(bitmap);
    }

    private void detectObject() {

        CONTOUR_COLOR = new Scalar(255);

        final MatOfKeyPoint keypoint = new MatOfKeyPoint();
        final List<KeyPoint> listpoint;
        KeyPoint kpoint;
        Mat mask = Mat.zeros(mGrey.size(), CvType.CV_8UC1);
        int rectanx1, rectany1, rectanx2, rectany2;
        int imgsize = mGrey.height() * mGrey.width();
        Scalar zeos = new Scalar(0, 0, 0);

        List<MatOfPoint> contour2 = new ArrayList<MatOfPoint>();
        Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();

        Rect rectan3;

        FeatureDetector detector = FeatureDetector
                .create(FeatureDetector.FAST);

        detector.detect(mGrey, keypoint);

        listpoint = keypoint.toList();


        for (int ind = 0; ind < listpoint.size(); ind++) {
            kpoint = listpoint.get(ind);
            rectanx1 = (int) (kpoint.pt.x - 0.5 * kpoint.size);
            rectany1 = (int) (kpoint.pt.y - 0.5 * kpoint.size);
            rectanx2 = (int) (kpoint.size);
            rectany2 = (int) (kpoint.size);
            if (rectanx1 <= 0)
                rectanx1 = 1;
            if (rectany1 <= 0)
                rectany1 = 1;
            if ((rectanx1 + rectanx2) > mGrey.width())
                rectanx2 = mGrey.width() - rectanx1;
            if ((rectany1 + rectany2) > mGrey.height())
                rectany2 = mGrey.height() - rectany1;
            Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
            try {
                Mat roi = new Mat(mask, rectant);
                roi.setTo(CONTOUR_COLOR);
            } catch (Exception ex) {
                Log.d("mylog", "mat roi error " + ex.getMessage());
            }
        }


        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel);
        Imgproc.findContours(morbyte, contour2, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        for (int ind = 0; ind < contour2.size(); ind++) {
            rectan3 = Imgproc.boundingRect(contour2.get(ind));

            if (rectan3.area() < imgsize * areaThreshold) {
                continue;
            }

            Bitmap bmp = null;
            try {
                Mat croppedPart;
                croppedPart = new Mat(mGrey, rectan3);
                bmp = Bitmap.createBitmap(croppedPart.width(), croppedPart.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(croppedPart, bmp);
                Log.d(TAG, "Cropping Successful");
            } catch (Exception e) {
                Log.d(TAG, "cropped part data error " + e.getMessage());
            }


            if (rectan3.area() > 0.5 * imgsize || rectan3.area() < 100
                    || rectan3.width / rectan3.height < 2) {
                Mat roi = new Mat(morbyte, rectan3);
                roi.setTo(zeos);

            } else {
                Imgproc.rectangle(mRgba, rectan3.br(), rectan3.tl(),
                        CONTOUR_COLOR);
            }

            if (bmp != null) {
                Log.d(TAG, "bitmap found!!");
            }
        }
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

}
