package com.flurry.sdk;

/* JADX INFO: loaded from: classes.dex */
public class mb {
    private static final String a = "mb";
    private static boolean b;

    public static synchronized void a() {
        if (b) {
            return;
        }
        kh.a((Class<? extends ki>) jd.class);
        try {
            kh.a((Class<? extends ki>) hk.class);
        } catch (NoClassDefFoundError unused) {
            kf.a(3, a, "Analytics module not available");
        }
        try {
            kh.a((Class<? extends ki>) lz.class);
        } catch (NoClassDefFoundError unused2) {
            kf.a(3, a, "Crash module not available");
        }
        try {
            kh.a((Class<? extends ki>) Class.forName("com.flurry.sdk.i"));
        } catch (ClassNotFoundException | NoClassDefFoundError unused3) {
            kf.a(3, a, "Ads module not available");
        }
        b = true;
    }
}
