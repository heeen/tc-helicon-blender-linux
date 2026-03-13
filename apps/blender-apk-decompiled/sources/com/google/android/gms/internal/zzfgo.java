package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzfgo {
    private static final Class<?> zzpnt = zztu("libcore.io.Memory");
    private static final boolean zzpnu;

    static {
        zzpnu = zztu("org.robolectric.Robolectric") != null;
    }

    static boolean zzcxm() {
        return (zzpnt == null || zzpnu) ? false : true;
    }

    static Class<?> zzcxn() {
        return zzpnt;
    }

    private static <T> Class<T> zztu(String str) {
        try {
            return (Class<T>) Class.forName(str);
        } catch (Throwable unused) {
            return null;
        }
    }
}
