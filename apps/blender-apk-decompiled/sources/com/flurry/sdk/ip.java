package com.flurry.sdk;

/* JADX INFO: loaded from: classes.dex */
public enum ip {
    GET("GET", 0),
    PUT("PUT", 1),
    POST("POST", 2);

    String d;
    int e;

    ip(String str, int i) {
        this.d = str;
        this.e = i;
    }

    public static ip a(int i) {
        switch (i) {
            case 0:
                return GET;
            case 1:
                return PUT;
            case 2:
                return POST;
            default:
                return null;
        }
    }
}
