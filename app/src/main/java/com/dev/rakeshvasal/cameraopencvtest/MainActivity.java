package com.dev.rakeshvasal.cameraopencvtest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ViewFlipper;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
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
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Used to load the 'native-lib' library on application startup.
    static {
        //System.loadLibrary("native-lib");
        if (!OpenCVLoader.initDebug()) {
            Log.d("MainActivity", "OpenCv Not Loaded");
        } else {
            Log.d("MainActivity", "OpenCv Loaded");
        }
    }

    BaseLoaderCallback mCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {

            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    javaCamera2View.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    private JavaCameraView javaCamera2View;
    private Button btn;
    private ViewFlipper viewFlipper;
    private RelativeLayout relativeLayout;
    private ImageView imageView;
    private Mat mRGBA, mCanny, mGray;
    private String TAG = "MainActivity";
    int count = 0;

    private findingrect asynk;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.javaCamera2View = (JavaCameraView) findViewById(R.id.OpenCvCamera);
        this.btn = (Button) findViewById(R.id.CaptureButton);

        this.relativeLayout = findViewById(R.id.CaptureLayout);
        //this.viewFlipper = findViewById(R.id.flipper);
        this.imageView = findViewById(R.id.CapturedImage);
        //viewFlipper.setDisplayedChild(0);
        javaCamera2View.setVisibility(SurfaceView.VISIBLE);
        javaCamera2View.setCvCameraViewListener(this);


        // Example of a call to a native method

    }


    @Override
    protected void onPause() {
        super.onPause();

        if (javaCamera2View != null) {
            javaCamera2View.disableView();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (javaCamera2View != null) {
            javaCamera2View.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("MainActivity", "OpenCv Not Loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mCallback);

        } else {
            Log.d("MainActivity", "OpenCv Loaded");
            mCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        this.mRGBA = new Mat(1280, 720, CvType.CV_8UC4);
        this.mCanny = new Mat(width, height, CvType.CV_8UC1);
        this.mGray = new Mat(1280, 720, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //System.gc();

        mRGBA = inputFrame.rgba();
        Mat image_output = new Mat();
        Mat grayCon = new Mat();

        Imgproc.cvtColor(mRGBA, grayCon, Imgproc.COLOR_BGR2GRAY);


        Mat blurred = grayCon.clone();
        Imgproc.medianBlur(mRGBA, blurred, 9);


        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        List<Mat> blurredChannel = new ArrayList<Mat>();
        blurredChannel.add(blurred);
        List<Mat> gray0Channel = new ArrayList<Mat>();
        gray0Channel.add(gray0);

        MatOfPoint2f approxCurve;

        double maxArea = 0;
        int maxId = -1;

        if (count == 0) {

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
                                (mRGBA.width() + mRGBA.height()) / 200, t);
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

                //if (maxArea > 10000 && maxArea < 15000) {
                //Log.d("Area Of Contour", ":" + maxArea);
                Log.d("Area Of Contour", ":" + maxArea);
                Imgproc.drawContours(mRGBA, contours, maxId, new Scalar(255, 0, 0,
                        .8), 10);


                       /* Rect rect = Imgproc.boundingRect(contours.get(maxId));


                        if (count == 0) {
                            image_output = mat[0].submat(rect);
                            Core.flip(image_output.t(), image_output, 1);
                            //sendInfo(mat[0],rect);
                            count = 1;

                            return image_output;

                        }*/

                Log.d(TAG, "Max Area Found");


                // }

            }
        }
           /* mat[0].release();
            gray.release();
            gray0.release();*/

        Log.d(TAG, "In Doing Background");

        //asynk = (findingrect) new findingrect().execute(mRGBA);


        return mRGBA;
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

    private void viewBitmap(Bitmap bitmap) {
        //mRGBA.release();
        //javaCamera2View.disableView();
        //relativeLayout.setVisibility(View.VISIBLE);
        //imageView.setImageBitmap(bitmap);
        //javaCamera2View.disableView();
        //asynk.cancel(true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        Intent intent = new Intent(this, DisplayImage.class);
        intent.putExtra("Bitmap", byteArray);
        startActivity(intent);
        asynk.cancel(true);
        javaCamera2View.disableView();
        finish();


    }

    private class findingrect extends AsyncTask<Mat, Mat, Mat> {


        @Override
        protected Mat doInBackground(Mat... mat) {

            Mat image_output = new Mat();
            Mat grayCon = new Mat();

            Imgproc.cvtColor(mat[0], grayCon, Imgproc.COLOR_BGR2GRAY);


            Mat blurred = grayCon.clone();
            Imgproc.medianBlur(mat[0], blurred, 9);


            Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            List<Mat> blurredChannel = new ArrayList<Mat>();
            blurredChannel.add(blurred);
            List<Mat> gray0Channel = new ArrayList<Mat>();
            gray0Channel.add(gray0);

            MatOfPoint2f approxCurve;

            double maxArea = 0;
            int maxId = -1;

            if (count == 0) {

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
                                    (mRGBA.width() + mRGBA.height()) / 200, t);
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

                    if (maxArea > 10000 && maxArea < 15000) {
                        //Log.d("Area Of Contour", ":" + maxArea);
                        Log.d("Area Of Contour", ":" + maxArea);
                        Imgproc.drawContours(mRGBA, contours, maxId, new Scalar(255, 0, 0,
                                .8), 10);


                        Rect rect = Imgproc.boundingRect(contours.get(maxId));


                        if (count == 0) {
                            image_output = mat[0].submat(rect);
                            Core.flip(image_output.t(), image_output, 1);
                            //sendInfo(mat[0],rect);
                            count = 1;

                            return image_output;

                        }

                        Log.d(TAG, "Max Area Found");


                    }

                }
            }
            mat[0].release();
            gray.release();
            gray0.release();
            return null;
        }


        @Override
        protected void onPostExecute(Mat image_output) {
            super.onPostExecute(image_output);


            /*if (image_output != null) {
                Log.d(TAG, "In Post with values");
                Bitmap resultBitmap = Bitmap.createBitmap(image_output.cols(), image_output.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(image_output, resultBitmap);
                viewBitmap(resultBitmap);

            }*/

        }
    }

    private void sendInfo(Mat mat, Rect rect) {

        Mat resultMat = mat.submat(rect);
        Bitmap bitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultMat, bitmap);
        resultMat.release();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        Intent intent = new Intent(this, DisplayImage.class);
        intent.putExtra("Bitmap", byteArray);
        startActivity(intent);
        finish();

    }


}
