package com.flurry.sdk;

import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
public final class jh {
    public static jh a;

    public static synchronized jh a() {
        if (a == null) {
            a = new jh();
        }
        return a;
    }

    private jh() {
    }

    public static String b() {
        return Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry();
    }
}
