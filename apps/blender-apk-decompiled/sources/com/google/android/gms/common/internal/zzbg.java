package com.google.android.gms.common.internal;

import android.support.annotation.Nullable;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbg {
    public static boolean equal(@Nullable Object obj, @Nullable Object obj2) {
        if (obj != obj2) {
            return obj != null && obj.equals(obj2);
        }
        return true;
    }

    public static zzbi zzx(Object obj) {
        return new zzbi(obj);
    }
}
