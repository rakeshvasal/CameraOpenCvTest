package com.dev.rakeshvasal.cameraopencvtest.CardRecognitionCode;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage;

class CommanUtils {

    public static String Directory = "";

    private static File getFileName() {

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory() + "/ImageFolder/");
        //File mediaStorageDir = new File("");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + timeStamp + ".jpg");

        return mediaFile;
    }

    static File getFileFromBitmap(Bitmap bitmap) {
        File f = getFileName();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            byte[] bitmapdata = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
        return f;
        //return null;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static Bitmap handleRotation(byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(new ByteArrayInputStream(data));
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            int rotationDegrees = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                    rotationDegrees = 90;
                    bitmap = rotateImage(bitmap, rotationDegrees);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationDegrees = 90;
                    bitmap = rotateImage(bitmap, rotationDegrees);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationDegrees = 180;
                    bitmap = rotateImage(bitmap, rotationDegrees);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationDegrees = 270;
                    bitmap = rotateImage(bitmap, rotationDegrees);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}
