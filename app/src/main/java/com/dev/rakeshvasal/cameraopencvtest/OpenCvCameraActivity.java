package com.dev.rakeshvasal.cameraopencvtest;

import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OpenCvCameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static String TAG = "CameraActivity";
    private CameraBridgeViewBase mOpenCvCameraView;
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
    private static double areaThreshold = 0.025; //threshold for the area size of an object
    private static Scalar CONTOUR_COLOR =null;

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
                    mOpenCvCameraView.enableView();
                    try {
                        initializeOpenCVDependencies();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

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
        mOpenCvCameraView.setCvCameraViewListener(this);
        tvName = (TextView) findViewById(R.id.text1);

        Button bt = findViewById(R.id.btn);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    private void initializeOpenCVDependencies() throws Exception {
        mOpenCvCameraView.enableView();
        detector = FeatureDetector.create(FeatureDetector.ORB);
        descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        img1 = new Mat();
        AssetManager assetManager = getAssets();
        InputStream istr = assetManager.open("a.jpeg");
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        Utils.bitmapToMat(bitmap, img1);
        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_RGB2GRAY);
        img1.convertTo(img1, 0); //converting the image to match with the type of the cameras image
        descriptors1 = new Mat();
        keypoints1 = new MatOfKeyPoint();
        detector.detect(img1, keypoints1);
        descriptor.compute(img1, keypoints1, descriptors1);

    }

    public Mat recognize(Mat aInputFrame) {
        try {
            Imgproc.cvtColor(aInputFrame, aInputFrame, Imgproc.COLOR_RGB2GRAY);
            descriptors2 = new Mat();
            keypoints2 = new MatOfKeyPoint();
            detector.detect(aInputFrame, keypoints2);
            descriptor.compute(aInputFrame, keypoints2, descriptors2);

            // Matching
            MatOfDMatch matches = new MatOfDMatch();
            if (img1.type() == aInputFrame.type()) {
                matcher.match(descriptors1, descriptors2, matches);
            } else {
                return aInputFrame;
            }
            List<DMatch> matchesList = matches.toList();

            Double max_dist = 0.0;
            Double min_dist = 100.0;

            for (int i = 0; i < matchesList.size(); i++) {
                Double dist = (double) matchesList.get(i).distance;
                if (dist < min_dist)
                    min_dist = dist;
                if (dist > max_dist)
                    max_dist = dist;
            }

            LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
            for (int i = 0; i < matchesList.size(); i++) {
                if (matchesList.get(i).distance <= (1.5 * min_dist))
                    good_matches.addLast(matchesList.get(i));
            }

            MatOfDMatch goodMatches = new MatOfDMatch();
            goodMatches.fromList(good_matches);
            Mat outputImg = new Mat();
            MatOfByte drawnMatches = new MatOfByte();
            if (aInputFrame.empty() || aInputFrame.cols() < 1 || aInputFrame.rows() < 1) {
                return aInputFrame;
            }
            Features2d.drawMatches(img1, keypoints1, aInputFrame, keypoints2, goodMatches, outputImg, GREEN, RED, drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
            Imgproc.resize(outputImg, outputImg, aInputFrame.size());
            return outputImg;
        } catch (Exception e) {
            e.printStackTrace();
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
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
        Log.d(TAG, " <- OnResume");
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        w = width;
        h = height;
        mGrey = new Mat(height, width, CvType.CV_8UC4);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //return nu
        detectObject();
        return inputFrame.rgba();
    }

    private void detectObject() {

        CONTOUR_COLOR = new Scalar(255);

        final MatOfKeyPoint keypoint = new MatOfKeyPoint();
        final List<KeyPoint> listpoint;
        KeyPoint kpoint;
        Mat mask = Mat.zeros(mGrey.size(), CvType.CV_8UC1);
        int rectanx1,rectany1,rectanx2,rectany2;
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

        listpoint= keypoint.toList();


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

            if(rectan3.area()<imgsize*areaThreshold){
                continue;
            }

            Bitmap bmp=null;
            try {
                Mat croppedPart;
                croppedPart = new Mat(mGrey,rectan3);
                bmp = Bitmap.createBitmap(croppedPart.width(), croppedPart.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(croppedPart, bmp);
                Log.d(TAG,"Cropping Successful");
            } catch (Exception e) {
                Log.d(TAG, "cropped part data error " + e.getMessage());
            }


            if (rectan3.area() > 0.5 * imgsize || rectan3.area() < 100
                    || rectan3.width / rectan3.height < 2) {
                Mat roi = new Mat(morbyte, rectan3);
                roi.setTo(zeos);

            }
            else
            {    Imgproc.rectangle(mRgba, rectan3.br(), rectan3.tl(),
                    CONTOUR_COLOR);
            }

            if (bmp != null) {
                Log.d(TAG,"bitmap found!!");
            }
        }
    }
}
