package com.dev.rakeshvasal.cameraopencvtest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.List;

public class ImagePreviewActivity extends AppCompatActivity {
    ImageView originalimageView, edgeimageView;
    Mat rgba;
    FirebaseVisionText[] firebaseVisionText1 = new FirebaseVisionText[1];
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        originalimageView = findViewById(R.id.original);
        edgeimageView = findViewById(R.id.edgeimage);
        String url = getIntent().getStringExtra("ImgURL");
        Bitmap image = BitmapFactory.decodeFile((new File(url)).getAbsolutePath());

        bitmap = image;
        detectEdges(image);
        detectUsingMlKit(image);

        /*Glide.with(ImagePreviewActivity.this)
                .load(url)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        //imageView.setImageBitmap(bitmap);
                        Bitmap edgebitmap = detectEdges(bitmap);

                        //imageView.setImageBitmap(edgebitmap);
                    }
                });*/
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
                                    showBitmap(croppedBitmap, edgeimageView);

                                    Point[] cornerPoints = block.getCornerPoints();
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
        showBitmap(bitmap, originalimageView);
        Bitmap resultBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, resultBitmap);
        showBitmap(resultBitmap, edgeimageView);
        return resultBitmap;
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
