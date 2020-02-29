package com.dev.rakeshvasal.cameraopencvtest.CardRecognitionCode;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dev.rakeshvasal.cameraopencvtest.ImageProcessingActivity;
import com.dev.rakeshvasal.cameraopencvtest.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JavaCameraViewActivity extends AppCompatActivity implements CameraLab.CameraCaptureCallbacks, CameraBridgeViewBase.CvCameraViewListener2 {


    CameraLab cameraView;
    private static int MAX_HEIGHT = 1600;
    private byte[] originalByteData;
    private static String TAG = "JavaCameraViewActivity.class";
    Bitmap originalBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_camera_view);
        cameraView = (CameraLab) findViewById(R.id.surface_view);
        Button bt = findViewById(R.id.btn);
        cameraView.setCaptureCallback(this);
        cameraView.setCvCameraViewListener(this);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.takePicture();
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
                    cameraView.enableView();
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
    public void onCapture(byte[] pictureByteData) {
        Mat mat = new Mat();
        if (pictureByteData != null) {
            Bitmap bitmap = CommanUtils.handleRotation(pictureByteData);
            if (bitmap != null) {
                originalBitmap = bitmap;
                originalByteData = pictureByteData;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float originalImageAspectRatio;
                if (width > height) {
                    originalImageAspectRatio = (float) width / height;
                } else if (height > width) {
                    originalImageAspectRatio = (float) height / width;
                    height = MAX_HEIGHT;
                    width = (int) (height / originalImageAspectRatio);
                    Log.i("ChangedWidth", "" + width);
                } else {
                    originalImageAspectRatio = 1;
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

                Utils.bitmapToMat(bitmap, mat);


                new CropCard().execute(mat);

            } else {
                Toast.makeText(this, "No Card Captured", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please Try Again", Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressLint("StaticFieldLeak")
    class CropCard extends AsyncTask<Mat, Mat, Mat> {
        Mat originalMat = new Mat();

        @Override
        protected Mat doInBackground(Mat... mat) {
            originalMat = mat[0];
            Mat image_output = new Mat();
            Mat grayCon = new Mat();
            int count = 0;

            Imgproc.cvtColor(mat[0], grayCon, Imgproc.COLOR_BGR2GRAY);

            Mat blurred = grayCon.clone();
            Imgproc.medianBlur(mat[0], blurred, 9);
            //originalBitmap = Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888);

            Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

            final List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

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
                            Imgproc.Canny(gray0, gray, 10, 30, 3, true); // true ?
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
                    double area = Imgproc.contourArea(contours.get(maxId));

                    final int finalMaxId = maxId;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Imgproc.drawContours(originalMat, contours, finalMaxId, new Scalar(255, 0, 0), 5);
                        }
                    });
                    Log.d("Area Of Contour", ":" + area);
                    if (maxId >= 0) {
                        //if (maxArea > 150000 && maxArea < 300000) {
                        if (maxArea > 100000 && maxArea < 500000) {

                            Log.d("Area Of Contour", ":" + maxArea);
                            org.opencv.core.Rect rect = Imgproc.boundingRect(contours.get(maxId));
                            if (count == 0) {
                                image_output = mat[0].submat(rect);
                                return image_output;
                            }
                            Log.d(TAG, "Max Area Found");
                        }
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
            if (image_output != null) {
                Log.d(TAG, "In Post with values");
                final Bitmap resultBitmap = Bitmap.createBitmap(image_output.cols(), image_output.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(image_output, resultBitmap);
                final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    }
                });

                File originalFile = CommanUtils.getFileFromBitmap(originalBitmap);
                File cardFile = CommanUtils.getFileFromBitmap(resultBitmap);


                Intent intent = new Intent(JavaCameraViewActivity.this, ImageProcessingActivity.class);
                intent.putExtra("ImgURL", originalFile.getAbsolutePath());
                //intent.putExtra("resultBitmap", cardFile.getAbsolutePath());
                startActivity(intent);
            } else {
                Toast.makeText(JavaCameraViewActivity.this, "No Card Captured", Toast.LENGTH_SHORT).show();
                if (originalByteData != null) {
                    Bitmap originalBitmap = CommanUtils.handleRotation(originalByteData);
                    originalBitmap = Bitmap.createScaledBitmap(originalBitmap, (originalBitmap.getWidth() - 100), (originalBitmap.getHeight() - 100), false);
                    originalByteData = null;
                }
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
        if (cameraView != null)
            cameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null)
            cameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return null;
    }


    private void CharacterRecognition(Mat sImage) {
        Mat grayImage = new Mat();
        Mat blurImage = new Mat();
        Mat thresImage = new Mat();
        Mat binImage = new Mat();
        Imgproc.cvtColor(sImage, grayImage, Imgproc.COLOR_BGR2GRAY); //градации серого
        Imgproc.GaussianBlur(grayImage, blurImage, new Size(5, 5), 0); //размытие
        Imgproc.adaptiveThreshold(blurImage, thresImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 101, 39);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        Imgproc.Canny(thresImage, binImage, 30, 10, 3, true); //контур
        Imgproc.findContours(binImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        hierarchy.release();
        Imgproc.drawContours(binImage, contours, -1, new Scalar(255, 255, 255));//, 2, 8, hierarchy, 0, new Point());


        MatOfPoint2f approxCurve = new MatOfPoint2f();

        //For each contour found
        for (int i = 0; i < contours.size(); i++) {
            //Convert contours(i) from MatOfPoint to MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint(approxCurve.toArray());


            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);

            // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
            Imgproc.rectangle(binImage, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 255, 255), 5);
        }
    }

    /*private void recognizeBitmap(Mat mRgba) {
        Log.i("Function", "recognizeBitmap");
        Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bitmap);
        FirebaseVisionLabelDetector detector = FirebaseVision.getInstance()
                .getVisionLabelDetector();

        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
        Task<List<FirebaseVisionLabel>> result =
                detector.detectInImage(firebaseVisionImage)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionLabel>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionLabel> labels) {
                                        for (FirebaseVisionLabel label : labels) {
                                            String text = label.getLabel();
                                            float confidence = label.getConfidence();
                                            Log.d("text1", text);
                                            Log.d("confidence", "" + confidence);
                                            if (text.equalsIgnoreCase("paper")) {
                                                Log.d("Lablel", text);
                                                Toast.makeText(JavaCameraViewActivity.this, "YO", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        e.printStackTrace();
                                    }
                                });
    }*/
}

