package com.dev.rakeshvasal.cameraopencvtest.CardRecognitionCode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import org.opencv.android.JavaCameraView;

import java.util.List;

@SuppressLint("ViewConstructor")
public class CameraLab extends JavaCameraView implements Camera.PictureCallback, SurfaceHolder.Callback {


    private byte[] pictureByteData;
    private CameraCaptureCallbacks cameraCaptureCallbacks;
    private Camera.Parameters params;

    public CameraLab(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        super.surfaceChanged(arg0, arg1, arg2, arg3);
        if (mCamera != null) {
            toggleFlash(false);
            mCamera.startPreview();
        }
    }

    public CameraLab(Context context, int cameraId) {
        super(context, cameraId);
    }

    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Camera.Size> getPictureSizeList() {
        return mCamera.getParameters().getSupportedPictureSizes();
    }

    public void setMaxResolution() {
        List<Camera.Size> sizes = getPictureSizeList();
        Camera.Size size = sizes.get(0);
        params.setPictureSize(size.width, size.height);
        mCamera.setParameters(params);
    }

    public Camera openFrontFacingCamera() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            Log.d("a", "Camera Info: " + cameraInfo.facing);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    return Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e("a", "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return null;
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void toggleFlash(boolean flashstate) {
        if (mCamera != null) {
            params = mCamera.getParameters();
            setMaxResolution();
            if (flashstate) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            } else {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(params);
        }
    }

    public void takePicture() {
        Log.i("CameraLab", "Taking pictureStart");
        mCamera.setPreviewCallback(null);
        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
        Log.i("CameraLab", "Taking pictureEnd");
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i("CameraLab", "onPictureTakenStart");
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);
        pictureByteData = data;
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        cameraCaptureCallbacks.onCapture(pictureByteData);
        Log.i("CameraLab", "onPictureTakenEnd");
    }

    public interface CameraCaptureCallbacks {
        public void onCapture(byte[] pictureByteData);
    }

    public void setCaptureCallback(CameraCaptureCallbacks captureCallback) {
        this.cameraCaptureCallbacks = captureCallback;
    }

}