package com.airbnb.lottie.utils;

/* JADX INFO: loaded from: classes.dex */
public class MeanCalculator {
    private int n;
    private float sum;

    public void add(float f) {
        this.sum += f;
        this.n++;
        if (this.n == Integer.MAX_VALUE) {
            this.sum /= 2.0f;
            this.n /= 2;
        }
    }

    public float getMean() {
        if (this.n == 0) {
            return 0.0f;
        }
        return this.sum / this.n;
    }
}
