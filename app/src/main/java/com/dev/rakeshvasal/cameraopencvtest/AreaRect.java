package com.dev.rakeshvasal.cameraopencvtest;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;

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
