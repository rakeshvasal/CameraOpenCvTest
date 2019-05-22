package com.dev.rakeshvasal.cameraopencvtest;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.List;

/**
 * Created by Rakeshvasal on 18-Apr-18.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback,Camera.PreviewCallback {
    private SurfaceHolder mHolder;
    public Camera mCamera;
    Context mContext;
    private WindowManager display;
    Camera.PreviewCallback previewCallback;

    private static final String cameraPreview = "CameraView";
    private static final String APP_CLASS = "APP_CLASS";
    private static final String Bug = "Bug";


    public CameraPreview(Context context, Camera mCamera) {
        super(context);

        mContext = context;

        this.mCamera = mCamera;
        mCamera.setDisplayOrientation(90);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(cameraPreview, "The failure of the camera settings");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            Camera.Size size = sizes.get(0);
            Camera.Size optionalSize = getOptimalPreviewSize(sizes, size.width, size.height);
            params.setPreviewSize(optionalSize.width, optionalSize.height);
            params.setRotation(90);
            params.setPictureSize(size.width, size.height);

            //setCameraDisplayOrientation(CameraActivity.this,cameraId,mCamera);
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            params.setExposureCompensation(0);
            params.setPictureFormat(ImageFormat.JPEG);
            params.setJpegQuality(100);
            mCamera.setPreviewCallback(this);
            try {
                mCamera.setParameters(params);
                previewCamera(holder);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(Bug, "setting Parameters Failed" + e.getLocalizedMessage());

            }
        }
    }

    public void previewCamera(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                boolean isPreviewRunning = true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(APP_CLASS, "Cannot start preview", e);
            }
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }



    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //Log.d("Frames", "" + data.length);
    }
}
