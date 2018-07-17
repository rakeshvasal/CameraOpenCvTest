package com.dev.rakeshvasal.cameraopencvtest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.Dimension;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImagePreviewActivity extends AppCompatActivity {
    ImageView originalimageView, edgeimageView, contoursview;
    Mat rgba;
    FirebaseVisionText[] firebaseVisionText1 = new FirebaseVisionText[1];
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        originalimageView = findViewById(R.id.original);
        edgeimageView = findViewById(R.id.edgeimage);
        contoursview = findViewById(R.id.contours);
        String url = getIntent().getStringExtra("ImgURL");
        Bitmap image = BitmapFactory.decodeFile((new File(url)).getAbsolutePath());

        bitmap = image;
        showBitmap(bitmap, originalimageView);

        detectEdges(image);
        //detectUsingMlKit(image);
        //detectLines(image);
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
                                    Rect boundingBox = block.getBoundingBox();
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

    private Bitmap detectEdges(Bitmap bitmap) {
        rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

        Mat edges = new Mat(rgba.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.Canny(edges, edges, 80, 100);

        // Don't do that at home or work it's for visualization purpose.
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Bitmap resultedgeBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, resultedgeBitmap);
        showBitmap(resultedgeBitmap, edgeimageView);
        Mat grayMat = new Mat(resultedgeBitmap.getHeight(), resultedgeBitmap.getWidth(), CvType.CV_8U, new Scalar(1));
        Imgproc.cvtColor(rgba, grayMat, Imgproc.COLOR_RGB2GRAY, 2);
        Imgproc.threshold(grayMat, grayMat, 100, 255, Imgproc.THRESH_BINARY);
        Core.bitwise_not(grayMat, grayMat);
        Imgproc.findContours(grayMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
       /* List<Point> pointList = new ArrayList<Point>();
        for (int i = 0; i < contours.size(); i++) {
            org.opencv.core.Rect rect = Imgproc.boundingRect(contours.get(i));

            double k = (rect.height + 0.0) / rect.width;
            if (0.9 < k && k < 1.1 && rect.area() > 100) {
                Imgproc.drawContours(grayMat, contours, i, new Scalar(0, 255, 0), 3);
            }
        }*/
        // now iterate over all top level contours
        ArrayList<Double> arrayList = new ArrayList<>();
        for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
            MatOfPoint matOfPoint = contours.get(idx);
            org.opencv.core.Rect rect = Imgproc.boundingRect(matOfPoint);

            arrayList.add(rect.area());
            //rect.area();
            Imgproc.rectangle(rgba, rect.tl(), rect.br(), new Scalar(0, 0, 255));
        }

        Double d = Collections.max(arrayList);
        int area = bitmap.getWidth() * bitmap.getHeight();
        Log.d("AREA ", "" + d);
        Log.d("AREA 1", "" + area);
        Bitmap resultconBitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, resultconBitmap);
        showBitmap(resultconBitmap, contoursview);
        return null;
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
