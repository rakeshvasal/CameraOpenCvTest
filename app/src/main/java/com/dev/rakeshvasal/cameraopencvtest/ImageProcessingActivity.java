package com.dev.rakeshvasal.cameraopencvtest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessingActivity extends AppCompatActivity {

    ImageView first, second, third, fourth, fifth, sixth;
    Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_processing);
        first = findViewById(R.id.first);
        second = findViewById(R.id.second);
        third = findViewById(R.id.third);
        fourth = findViewById(R.id.fourth);
        fifth = findViewById(R.id.fifth);
        sixth = findViewById(R.id.sixth);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            String url = getIntent().getStringExtra("ImgURL");
            mBitmap = BitmapFactory.decodeFile((new File(url)).getAbsolutePath());
            //ProcessImage(mBitmap);
            Mat originalMat = new Mat();
            Utils.bitmapToMat(mBitmap, originalMat);
            showBitmap(originalMat, first);
            try {
                //findRectangle(originalMat);
                new AsyncProcess(originalMat).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void ProcessImage(Bitmap bitmap) {

        Bitmap originalbitmap = bitmap;

        Mat originalMat = new Mat();
        Mat grayMat = new Mat();
        Mat thresholdMat = new Mat();
        Mat cannyMat = new Mat();
        Mat linesMat = new Mat();
        Mat contourMat = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        ///
        Utils.bitmapToMat(originalbitmap, originalMat);
        contourMat = originalMat.clone();
        showBitmap(originalMat, first);
        //convert to gray start
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGB2GRAY, 2);
        showBitmap(grayMat, second);

        Imgproc.adaptiveThreshold(grayMat, thresholdMat, 255, 1, 1, 75, 10);
        showBitmap(thresholdMat, third);

        /*Imgproc.Canny(thresholdMat, cannyMat, 30, 40, 3, true);
        showBitmap(cannyMat,fourth);*/

        Imgproc.HoughLinesP(thresholdMat, linesMat, 1, Math.PI / 180, 50, 20, 20);
        // showBitmap(linesMat,fifth);
        for (int y = 0; y < linesMat.rows(); y++) {
            double[] vec = linesMat.get(y, 0);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            Log.d("start", "" + start.x + ":" + start.y);
            Log.d("end", "" + end.x + ":" + end.y);
            Imgproc.line(grayMat, start, end, new Scalar(255, 0, 0), 3);
        }
        showBitmap(grayMat, fifth);

        Imgproc.findContours(grayMat, contours, new Mat(),
                Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Log.d("ContourCount", "" + contours.size());
        int savedContour = -1;
        double maxArea = 0.0;
        MatOfPoint2f approxCurve;
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
                    savedContour = contours.indexOf(contour);
                }
            }
        }
        if (savedContour > 0) {
            Log.d("maxContourArea", "" + Imgproc.contourArea(contours.get(savedContour)));
            Rect rect = Imgproc.boundingRect(contours.get(savedContour));
        }
        Imgproc.drawContours(contourMat, contours, savedContour, new Scalar(255, 0, 0,
                .8), 15);
        showBitmap(contourMat, sixth);
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
        showBitmap(src, second);
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

                if (maxArea > 30000 && maxArea < 40000) {
                    //Log.d("Area Of Contour", ":" + maxArea);
                    Log.d("Area Of Contour", ":" + maxArea);
               /* Imgproc.drawContours(mRGBA, contours, maxId, new Scalar(255, 0, 0,
                        .8), 10);*/
                    Rect rect = Imgproc.boundingRect(contours.get(maxId));
                    finalMat = originalMat.submat(rect);
                    //Core.flip(image_output.t(), image_output, 1);
                    //sendInfo(mat[0],rect);
                    //return image_output;
                    Log.d("TAG", "Max Area Found");
                }

            }

            originalMat.release();
            gray.release();
            gray0.release();

        }

        @Override
        protected void onPostExecute(String s) {
            //showBitmap(finalMat, second);
            super.onPostExecute(s);
        }
    }
}
