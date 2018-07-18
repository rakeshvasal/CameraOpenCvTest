package com.dev.rakeshvasal.cameraopencvtest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import android.graphics.PointF;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.Dimension;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.co.senab.photoview.PhotoView;

public class ImagePreviewActivity extends AppCompatActivity {
    ImageView originalimageView, edgeimageView, contoursview, fourth, fifth, sixth;
    Mat rgba;
    FirebaseVisionText[] firebaseVisionText1 = new FirebaseVisionText[1];
    Bitmap bitmap;
    QuadrilateralView mSelectionImageView;
    private static final int MAX_HEIGHT = 500;
    Bitmap mBitmap, mResult;
    MaterialDialog mResultDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        originalimageView = findViewById(R.id.original);
        edgeimageView = findViewById(R.id.edgeimage);
        contoursview = findViewById(R.id.contours);
        fourth = findViewById(R.id.fourth);
        fifth = findViewById(R.id.fifth);
        sixth = findViewById(R.id.sixth);
        mSelectionImageView = (QuadrilateralView) findViewById(R.id.polygonView);
        String url = getIntent().getStringExtra("ImgURL");
        mBitmap = BitmapFactory.decodeFile((new File(url)).getAbsolutePath());
        mSelectionImageView.setImageBitmap(getResizedBitmap(mBitmap, MAX_HEIGHT));
        List<PointF> points = findPoints();
        mSelectionImageView.setPoints(points);
        bitmap = mBitmap;
        Button btn = findViewById(R.id.btn);
        mResultDialog = new MaterialDialog.Builder(this)
                .title("Scan Result")
                .positiveText("Save")
                .negativeText("Cancel")
                .customView(R.layout.dialog_document_scan_result, false)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        // TODO Saving
                        mResult = null;
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        mResult = null;
                    }
                })
                .build();
        //showBitmap(bitmap, originalimageView);

        //detectEdges(image);
        //detectUsingMlKit(image);
        //detectLines(image);
        //processImage(mBitmap);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectImage();
            }
        });

    }

    /**
     * Attempt to find the four corner points for the largest contour in the image.
     *
     * @return A list of points, or null if a valid rectangle cannot be found.
     */
    private List<PointF> findPoints() {
        List<PointF> result = null;

        Mat image = new Mat();
        Mat orig = new Mat();
        org.opencv.android.Utils.bitmapToMat(getResizedBitmap(mBitmap, MAX_HEIGHT), image);
        org.opencv.android.Utils.bitmapToMat(mBitmap, orig);

        Mat edges = edgeDetection(image);
        MatOfPoint2f largest = findLargestContour(edges);

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

        edges.release();
        image.release();
        orig.release();

        return result;
    }

    /**
     * Sort the points
     * <p>
     * The order of the points after sorting:
     * 0------->1
     * ^        |
     * |        v
     * 3<-------2
     * <p>
     * NOTE:
     * Based off of http://www.pyimagesearch.com/2014/08/25/4-point-opencv-getperspective-transform-example/
     *
     * @param src The points to sort
     * @return An array of sorted points
     */
    private Point[] sortPoints(Point[] src) {
        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));
        Point[] result = {null, null, null, null};

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };
        Comparator<Point> differenceComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        result[0] = Collections.min(srcPoints, sumComparator);        // Upper left has the minimal sum
        result[2] = Collections.max(srcPoints, sumComparator);        // Lower right has the maximal sum
        result[1] = Collections.min(srcPoints, differenceComparator); // Upper right has the minimal difference
        result[3] = Collections.max(srcPoints, differenceComparator); // Lower left has the maximal difference

        return result;
    }

    /**
     * Find the largest 4 point contour in the given Mat.
     *
     * @param src A valid Mat
     * @return The largest contour as a Mat
     */
    private MatOfPoint2f findLargestContour(Mat src) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Get the 5 largest contours
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                double area1 = Imgproc.contourArea(o1);
                double area2 = Imgproc.contourArea(o2);
                return (int) (area2 - area1);
            }
        });
        if (contours.size() > 5) contours.subList(4, contours.size() - 1).clear();

        MatOfPoint2f largest = null;
        for (MatOfPoint contour : contours) {
            MatOfPoint2f approx = new MatOfPoint2f();
            MatOfPoint2f c = new MatOfPoint2f();
            contour.convertTo(c, CvType.CV_32FC2);
            Imgproc.approxPolyDP(c, approx, Imgproc.arcLength(c, true) * 0.02, true);

            if (approx.total() == 4 && Imgproc.contourArea(contour) > 150) {
                // the contour has 4 points, it's valid
                largest = approx;
                break;
            }
        }

        return largest;
    }

    /**
     * Detect the edges in the given Mat
     *
     * @param src A valid Mat object
     * @return A Mat processed to find edges
     */
    private Mat edgeDetection(Mat src) {
        Mat edges = new Mat();
        Imgproc.cvtColor(src, edges, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(edges, edges, new Size(5, 5), 0);
        Imgproc.Canny(edges, edges, 75, 200);
        return edges;
    }

    private void detectImage() {
        List<PointF> points = mSelectionImageView.getPoints();

        if (mBitmap != null) {
            Mat orig = new Mat();
            org.opencv.android.Utils.bitmapToMat(mBitmap, orig);

            Mat transformed = perspectiveTransform(orig, points);
            mResult = applyThreshold(transformed);

            PhotoView photoView = (PhotoView) mResultDialog.getCustomView().findViewById(R.id.imageView);
            photoView.setImageBitmap(mResult);
            mResultDialog.show();
            showBitmap(mBitmap,originalimageView);
            showBitmap(mResult,contoursview);

        }
    }

    /**
     * Apply a threshold to give the "scanned" look
     * <p>
     * NOTE:
     * See the following link for more info http://docs.opencv.org/3.1.0/d7/d4d/tutorial_py_thresholding.html#gsc.tab=0
     *
     * @param src A valid Mat
     * @return The processed Bitmap
     */
    private Bitmap applyThreshold(Mat src) {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);

        // Some other approaches
//        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);
//        Imgproc.threshold(src, src, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        Imgproc.GaussianBlur(src, src, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

        Bitmap bm = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(src, bm);

        return bm;
    }

    /**
     * Transform the coordinates on the given Mat to correct the perspective.
     *
     * @param src    A valid Mat
     * @param points A list of coordinates from the given Mat to adjust the perspective
     * @return A perspective transformed Mat
     */
    private Mat perspectiveTransform(Mat src, List<PointF> points) {
        Point point1 = new Point(points.get(0).x, points.get(0).y);
        Point point2 = new Point(points.get(1).x, points.get(1).y);
        Point point3 = new Point(points.get(2).x, points.get(2).y);
        Point point4 = new Point(points.get(3).x, points.get(3).y);
        Point[] pts = {point1, point2, point3, point4};
        return fourPointTransform(src, sortPoints(pts));
    }

    /**
     * NOTE:
     * Based off of http://www.pyimagesearch.com/2014/08/25/4-point-opencv-getperspective-transform-example/
     *
     * @param src
     * @param pts
     * @return
     */
    private Mat fourPointTransform(Mat src, Point[] pts) {
        double ratio = src.size().height / (double) MAX_HEIGHT;

        Point ul = pts[0];
        Point ur = pts[1];
        Point lr = pts[2];
        Point ll = pts[3];

        double widthA = Math.sqrt(Math.pow(lr.x - ll.x, 2) + Math.pow(lr.y - ll.y, 2));
        double widthB = Math.sqrt(Math.pow(ur.x - ul.x, 2) + Math.pow(ur.y - ul.y, 2));
        double maxWidth = Math.max(widthA, widthB) * ratio;

        double heightA = Math.sqrt(Math.pow(ur.x - lr.x, 2) + Math.pow(ur.y - lr.y, 2));
        double heightB = Math.sqrt(Math.pow(ul.x - ll.x, 2) + Math.pow(ul.y - ll.y, 2));
        double maxHeight = Math.max(heightA, heightB) * ratio;

        Mat resultMat = new Mat(Double.valueOf(maxHeight).intValue(), Double.valueOf(maxWidth).intValue(), CvType.CV_8UC4);

        Mat srcMat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstMat = new Mat(4, 1, CvType.CV_32FC2);
        srcMat.put(0, 0, ul.x * ratio, ul.y * ratio, ur.x * ratio, ur.y * ratio, lr.x * ratio, lr.y * ratio, ll.x * ratio, ll.y * ratio);
        dstMat.put(0, 0, 0.0, 0.0, maxWidth, 0.0, maxWidth, maxHeight, 0.0, maxHeight);

        Mat M = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Imgproc.warpPerspective(src, resultMat, M, resultMat.size());

        srcMat.release();
        dstMat.release();
        M.release();

        return resultMat;
    }

    /**
     * Resize a given bitmap to scale using the given height
     *
     * @return The resized bitmap
     */
    private Bitmap getResizedBitmap(Bitmap bitmap, int maxHeight) {
        double ratio = bitmap.getHeight() / (double) maxHeight;
        int width = (int) (bitmap.getWidth() / ratio);
        return Bitmap.createScaledBitmap(bitmap, width, maxHeight, false);
    }

    private void detectUsingMlKit(final Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionLabelDetector detector = FirebaseVision.getInstance()
                .getVisionLabelDetector();


// Or, to set the minimum confidence required:
       /* FirebaseVisionLabelDetector detector = FirebaseVision.getInstance()
                .getVisionLabelDetector(options);*/


        Task<List<FirebaseVisionLabel>> result =
                detector.detectInImage(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionLabel>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionLabel> labels) {
                                        for (FirebaseVisionLabel label : labels) {
                                            String text = label.getLabel();
                                            String entityId = label.getEntityId();
                                            float confidence = label.getConfidence();
                                            Log.d("text", text);
                                            Log.d("entityId", entityId);
                                            Log.d("confidence", "" + confidence);
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
        FirebaseVisionTextDetector textdetector = FirebaseVision.getInstance()
                .getVisionTextDetector();


        Task<FirebaseVisionText> result1 =
                textdetector.detectInImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {

                                for (FirebaseVisionText.Block block : firebaseVisionText.getBlocks()) {
                                    android.graphics.Rect boundingBox = block.getBoundingBox();
                                    Log.d("boundingBox", "" + boundingBox.top + ":" + boundingBox.left + ":" + boundingBox.right + ":" + boundingBox.bottom);

                                    int width = boundingBox.right - boundingBox.left;
                                    int height = boundingBox.bottom - boundingBox.top;
                                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, boundingBox.left, boundingBox.top, width, height);
                                    //showBitmap(croppedBitmap, edgeimageView);

                                    android.graphics.Point[] cornerPoints = block.getCornerPoints();
                                    Log.d("boundingBox", "" + cornerPoints);
                                    String text = block.getText();
                                    for (FirebaseVisionText.Line line : block.getLines()) {
                                        for (FirebaseVisionText.Element element : line.getElements()) {

                                        }
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
    }

    private void processImage(Bitmap bitmap) {
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
        Scalar white = new Scalar(0, 255, 255);

// draw contours border and fill them
        Mat contourMat = originalMat;
        Imgproc.drawContours(contourMat, contours, -1, white, 10);
        int i = 0;
        for (MatOfPoint contour : contours) {
            //Imgproc.drawContours(contourMat, contours, i, white, 10);
            //Imgproc.fillPoly(contourMat, Arrays.asList(contour), white);
        }

        Bitmap contoursbitmap = Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888);
        i = 0;
        Utils.matToBitmap(contourMat, contoursbitmap);

        showBitmap(contoursbitmap, fourth);

        for (int j = 0; j < contours.size(); j++) {
            double area = contourArea(contours.get(j));
            if (area > largest_area) {
                largest_area = area;
                second_large_area = area;
                largest_contour_index = j;
            }
            bounding_rect = Imgproc.boundingRect(contours.get(j));
        }

    }

    private Bitmap detectEdges(Bitmap bitmap) {
        double largest_area = 0;
        double second_large_area = 0;
        int largest_contour_index = 0;

        org.opencv.core.Rect bounding_rect = null;

        Mat originalMat = new Mat();

        Utils.bitmapToMat(bitmap, originalMat);


        Mat edges = new Mat(originalMat.size(), CvType.CV_8UC1);

        Imgproc.cvtColor(originalMat, edges, Imgproc.COLOR_RGB2GRAY, 4);

        Imgproc.Canny(originalMat, edges, 30, 100);

        // Don't do that at home or work it's for visualization purpose.
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierarchy = new Mat();

        Bitmap resultedgeBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(edges, resultedgeBitmap);


        showBitmap(resultedgeBitmap, edgeimageView);

        Mat grayMat = new Mat(resultedgeBitmap.getHeight(), resultedgeBitmap.getWidth(), CvType.CV_8U, new Scalar(1));

        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGB2GRAY, 2);

        Imgproc.threshold(grayMat, grayMat, 100, 255, Imgproc.THRESH_BINARY);

        Core.bitwise_not(grayMat, grayMat);


        Bitmap graybitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(grayMat, graybitmap);

        showBitmap(graybitmap, edgeimageView);

        Imgproc.findContours(grayMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        ///////Find Edges of Grayscale image

        Mat graymat = new Mat();

        Utils.bitmapToMat(graybitmap, graymat);

        Mat graybitmapedges = new Mat(originalMat.size(), CvType.CV_8UC1);

        Imgproc.cvtColor(graymat, graybitmapedges, Imgproc.COLOR_RGB2GRAY, 4);

        Imgproc.Canny(graybitmapedges, graybitmapedges, 30, 100);

        Mat histogramqualizedMat = new Mat(originalMat.size(), CvType.CV_8UC1);

        Imgproc.equalizeHist(graybitmapedges, histogramqualizedMat);

        Bitmap graymatedgegeBitmap = Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(edges, graymatedgegeBitmap);

        Bitmap hitoGramEqualizedBitmap = Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(histogramqualizedMat, hitoGramEqualizedBitmap);
        int type = 1;
        if (type == 0) {
            List<MatOfPoint> histogramMatcontours = new ArrayList<MatOfPoint>();
            Mat histogramMahierarchy = new Mat();
            Imgproc.findContours(histogramqualizedMat, histogramMatcontours, histogramMahierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            for (int i = 0; i < histogramMatcontours.size(); i++) {
                double area = contourArea(histogramMatcontours.get(i));
                if (area > largest_area) {
                    largest_area = area;
                    second_large_area = area;
                    largest_contour_index = i;
                }
                bounding_rect = Imgproc.boundingRect(histogramMatcontours.get(i));
                AreaRect areaRect = new AreaRect(contourArea(histogramMatcontours.get(i)), bounding_rect, histogramMatcontours.get(i));

            }
            Imgproc.drawContours(originalMat, histogramMatcontours, largest_contour_index, new Scalar(0, 255, 150), 15);
        } else {
            for (int i = 0; i < contours.size(); i++) {
                double area = contourArea(contours.get(i));
                if (area > largest_area) {
                    largest_area = area;
                    second_large_area = area;
                    largest_contour_index = i;
                }
                bounding_rect = Imgproc.boundingRect(contours.get(i));
                AreaRect areaRect = new AreaRect(contourArea(contours.get(i)), bounding_rect, contours.get(i));

            }

        }
        contours.remove(largest_contour_index);
        largest_area = 0;
        for (int i = 0; i < contours.size(); i++) {
            double area = contourArea(contours.get(i));
            if (area > largest_area) {
                largest_area = area;

                largest_contour_index = i;
            }
            bounding_rect = Imgproc.boundingRect(contours.get(i));
            AreaRect areaRect = new AreaRect(contourArea(contours.get(i)), bounding_rect, contours.get(i));

        }

        //Imgproc.drawContours(originalMat, contours, largest_contour_index, new Scalar(0, 255, 150), 5);

        org.opencv.core.Rect maxCotourRect = Imgproc.boundingRect(contours.get(largest_contour_index));

        Bitmap bitmapwithcontour = Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888);

        Bitmap histogrambitmapwithcontour = Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(originalMat, bitmapwithcontour);

        Utils.matToBitmap(histogramqualizedMat, histogrambitmapwithcontour);

        //showBitmap(histogrambitmapwithcontour, contoursview);

        showBitmap(bitmapwithcontour, fourth);

        Bitmap croppedbitmap = Bitmap.createBitmap(originalMat.cols(), originalMat.rows(), Bitmap.Config.ARGB_8888);

        croppedbitmap = Bitmap.createBitmap(bitmap, maxCotourRect.x, maxCotourRect.y, maxCotourRect.width, maxCotourRect.height);

        showBitmap(croppedbitmap, fourth);
        return null;
    }


    private double contourArea(MatOfPoint contour) {
        return Imgproc.contourArea(contour);
    }

    private void detectLines(Bitmap bitmap) {
        Mat mat = new Mat();
        Mat edges = new Mat();
        Mat mRgba = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
        Mat lines = new Mat();

        Utils.bitmapToMat(bitmap, mat);

        Imgproc.Canny(mat, edges, 50, 90);


        int threshold = 50;
        int minLineSize = 20;
        int lineGap = 20;

        Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, threshold, minLineSize, lineGap);

        for (int y = 0; y < lines.rows(); y++) {
            double[] vec = lines.get(y, 0);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            Log.d("start", "" + start.x + ":" + start.y);
            Log.d("end", "" + end.x + ":" + end.y);
            Imgproc.line(mRgba, start, end, new Scalar(255, 0, 0), 3);
        }

        Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(mRgba, bmp);
        bitmap = bmp;
        showBitmap(bitmap, edgeimageView);
    }

    private void showBitmap(Bitmap bitmap, ImageView imageView) {
        imageView.setImageBitmap(bitmap);
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        Intent intent = new Intent();
        intent.putExtra("image_path", "abc");
        setResult(RESULT_OK, intent);
        finish();

    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    rgba = new Mat();
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
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


}
