package com.google.android.gms.common.internal;

import android.os.Looper;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzc {
    public static void checkState(boolean z) {
        if (!z) {
            throw new IllegalStateException();
        }
    }

    public static void zzb(Object obj, Object obj2) {
        if (obj == null) {
            throw new IllegalArgumentException(String.valueOf(obj2));
        }
    }

    public static void zzgn(String str) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            return;
        }
        String strValueOf = String.valueOf(Thread.currentThread());
        String strValueOf2 = String.valueOf(Looper.getMainLooper().getThread());
        StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 57 + String.valueOf(strValueOf2).length());
        sb.append("checkMainThread: current thread ");
        sb.append(strValueOf);
        sb.append(" IS NOT the main thread ");
        sb.append(strValueOf2);
        sb.append("!");
        Log.e("Asserts", sb.toString());
        throw new IllegalStateException(str);
    }

    public static void zzv(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("null reference");
        }
    }
}
