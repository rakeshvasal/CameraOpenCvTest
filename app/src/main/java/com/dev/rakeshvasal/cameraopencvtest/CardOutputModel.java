package com.dev.rakeshvasal.cameraopencvtest;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class CardOutputModel implements Parcelable {

    private Bitmap originalBitmap;
    private Bitmap cardBitmap;

    public CardOutputModel(Bitmap originalBitmap, Bitmap cardBitmap) {
        this.originalBitmap = originalBitmap;
        this.cardBitmap = cardBitmap;
    }

    public Bitmap getOriginalBitmap() {
        return originalBitmap;
    }

    public void setOriginalBitmap(Bitmap originalBitmap) {
        this.originalBitmap = originalBitmap;
    }

    public Bitmap getCardBitmap() {
        return cardBitmap;
    }

    public void setCardBitmap(Bitmap cardBitmap) {
        this.cardBitmap = cardBitmap;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }
}
