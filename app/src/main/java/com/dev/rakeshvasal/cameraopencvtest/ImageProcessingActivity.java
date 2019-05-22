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
            ProcessImage(mBitmap);
            Mat originalMat = new Mat();
            Utils.bitmapToMat(mBitmap, originalMat);
            showBitmap(originalMat, first);
            try {
                //new AsyncProcess(originalMat).execute();
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

        Imgproc.Canny(thresholdMat, cannyMat, 30, 40, 3, true);
        showBitmap(cannyMat,fourth);

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

    /*private void processImage(Bitmap bitmap) {
        double largest_area = 0;

        double second_large_area = 0;

        int largest_contour_index = 0;

        Rect bounding_rect = null;

        Mat originalMat = new Mat();

        Utils.bitmapToMat(bitmap, originalMat);

        Mat GrayMat = new Mat(originalMat.size(), CvType.CV_8UC1);
        //convert to gray start
        Imgproc.cvtColor(originalMat, GrayMat, Imgproc.COLOR_RGB2GRAY, 2);

        Bitmap RGB2GrayBitmap = Bitmap.createBitmap(GrayMat.cols(), GrayMat.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(GrayMat, RGB2GrayBitmap);

        showBitmap(RGB2GrayBitmap, originalimageView);
        //convert to gray done

        //finding edges start we use the same gray mat
        Mat edgeMat = new Mat(originalMat.size(), CvType.CV_8UC1);

        Imgproc.Canny(GrayMat, edgeMat, 30, 100);

        Bitmap EdgeBitmap = Bitmap.createBitmap(edgeMat.cols(), edgeMat.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(edgeMat, EdgeBitmap);

        showBitmap(EdgeBitmap, edgeimageView);
        //finding edges done

        //histogram equalization
        Mat histogramqualizedMat = new Mat(originalMat.size(), CvType.CV_8UC1);

        Imgproc.equalizeHist(GrayMat, histogramqualizedMat);

        Bitmap histogramequalizedBitmap = Bitmap.createBitmap(edgeMat.cols(), edgeMat.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(histogramqualizedMat, histogramequalizedBitmap);

        showBitmap(histogramequalizedBitmap, contoursview);

        //finding contours start we use the
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierarchy = new Mat();

        Imgproc.threshold(histogramqualizedMat, histogramqualizedMat, 100, 255, Imgproc.THRESH_BINARY);

        Core.bitwise_not(histogramqualizedMat, histogramqualizedMat);

        Imgproc.findContours(histogramqualizedMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // create Scalar for color of mask objects
        Scalar white = new Scalar(0, 0, 255);

        // draw contours border and fill them
        Mat contourMat = originalMat;
        Imgproc.drawContours(contourMat, contours, -1, white, 10);

        Bitmap contoursbitmap = Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(contourMat, contoursbitmap);
        showBitmap(contoursbitmap, fourth);
        //contoes draw ends

        //Gussian blur
        Bitmap gaussianblurbitmap = Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888);
        Mat guassianblurmat = originalMat;
        org.opencv.core.Size s = new Size(3, 3);
        Imgproc.GaussianBlur(guassianblurmat, guassianblurmat, s, 2);
        Utils.matToBitmap(guassianblurmat, gaussianblurbitmap);
        //showBitmap(gaussianblurbitmap, fifth);

        //threshold finding
        Bitmap thresholdbitmap = Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888);
        Imgproc.adaptiveThreshold(GrayMat, GrayMat, 255, 1, 1, 75, 10);
        Utils.matToBitmap(GrayMat, thresholdbitmap);
        showBitmap(thresholdbitmap, fifth);
        //NPE Check
        List<PointF> points = findPoints(thresholdbitmap);
        if (points!=null){
            for (int i = 0; i < points.size(); i++) {
                Log.i("Points0", "" + points.get(0));
                Log.i("Points1", "" + points.get(1));
            }
        }

        //end threshold
        // detectLines(thresholdbitmap);

        //findLines(originalMat);

        *//*MatOfPoint2f largest = findLargestContour(GrayMat);
        List<PointF> result = null;
        if (largest != null) {
            Point[] points = sortPoints(largest.toArray());
            result = new ArrayList<>();
            result.add(new PointF(Double.valueOf(points[0].x).floatValue(), Double.valueOf(points[0].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[1].x).floatValue(), Double.valueOf(points[1].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[2].x).floatValue(), Double.valueOf(points[2].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[3].x).floatValue(), Double.valueOf(points[3].y).floatValue()));
            largest.release();
        } else {
            //Timber.d("Can't find rectangle!");
        }
        List<PointF> resultpoints = result;*//*
        //Imgproc.drawContours(contourMat, matOfPoint2f, -1, white, 10);
        //Rect rect = Imgproc.boundingRect(matOfPoint2f);

        //apply percpective
        //approach 1
        Mat mat = originalMat;
        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);


        src_mat.put(0, 0, 407.0, 74.0, 1606.0, 74.0, 420.0, 2589.0, 1698.0, 2589.0);
        dst_mat.put(0, 0, 0.0, 0.0, 1600.0, 0.0, 0.0, 2500.0, 1600.0, 2500.0);
        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

        Mat dst = mat.clone();

        Imgproc.warpPerspective(mat, dst, perspectiveTransform, new Size(bitmap.getWidth(), bitmap.getHeight()));

        //approcah 2
        Mat M = new Mat(3, 3, CvType.CV_32F);
        Size size = new Size(200.0, 200.0);
        Mat outputMat = GrayMat.clone();
        Scalar scalar = new Scalar(50.0);
        Imgproc.warpPerspective(GrayMat, outputMat, M, size, Imgproc.INTER_LINEAR + Imgproc.CV_WARP_FILL_OUTLIERS, BORDER_DEFAULT, scalar);
        Bitmap percpectiveBitmap = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(outputMat, percpectiveBitmap);
        Utils.matToBitmap(dst, percpectiveBitmap);
        showBitmap(percpectiveBitmap, originalimageView);
        //end percpective
    }*/

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
