package com.dev.rakeshvasal.cameraopencvtest;

import android.support.annotation.NonNull;

import org.opencv.core.Rect;

public class AreaRect implements Comparable {

    double areaDouble;
    Rect rect;

    public AreaRect(double area, Rect rect) {
        this.areaDouble = area;
        this.rect = rect;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        return (int) rect.area();
    }

    public Rect getRect() {
        return rect;
    }
}
