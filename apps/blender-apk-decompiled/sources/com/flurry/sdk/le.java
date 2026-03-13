package com.flurry.sdk;

import android.content.Context;
import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes.dex */
public final class le extends jz {
    public WeakReference<Context> a;
    public ld b;
    public int c;
    public long d;

    /* JADX WARN: $VALUES field not found */
    /* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
    public static final class a {
        public static final int a = 1;
        public static final int b = 2;
        public static final int c = 3;
        public static final int d = 4;
        public static final int e = 5;
        private static final /* synthetic */ int[] f = {a, b, c, d, e};

        public static int[] a() {
            return (int[]) f.clone();
        }
    }

    public le() {
        super("com.flurry.android.sdk.FlurrySessionEvent");
    }
}
