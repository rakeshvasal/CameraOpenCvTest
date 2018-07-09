package com.dev.rakeshvasal.cameraopencvtest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Dimension;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class ImagePreviewActivity extends AppCompatActivity {
    ImageView originalimageView, edgeimageView;
    Mat rgba;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        originalimageView = findViewById(R.id.original);
        edgeimageView = findViewById(R.id.edgeimage);
        String url = getIntent().getStringExtra("ImgURL");
        Bitmap image = BitmapFactory.decodeFile((new File(url)).getAbsolutePath());
        detectEdges(image);
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
