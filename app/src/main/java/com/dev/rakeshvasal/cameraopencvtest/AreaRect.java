package com.dev.rakeshvasal.cameraopencvtest;

import android.support.annotation.NonNull;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class AreaRect {

    double areaDouble;
    Rect rect;
    MatOfPoint contour;

    public AreaRect(double area, Rect rect, MatOfPoint contour) {
        this.areaDouble = area;
        this.rect = rect;
        this.contour = contour;
    }
    public Rect getRect() {
        return rect;
    }
}
