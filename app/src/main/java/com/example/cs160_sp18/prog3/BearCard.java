package com.example.cs160_sp18.prog3;

import android.net.Uri;

public class BearCard {
    public String name;
    public int distance;
    public Uri image;

    BearCard(String name, int distance, Uri image) {
        this.name = name;
        this.distance = distance;
        this.image = image;
    }

    protected String distanceString() {
        if (distance <= 10) {
            return "<10 meters";
        } else if (distance < 1000) {
            return Integer.toString(distance) + " meters";
        } else {
            int kms = distance / 1000;
            return Integer.toString(kms) + " kms";
        }
    }
}
