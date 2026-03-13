package com.google.android.gms.internal;

import android.os.Looper;
import android.support.annotation.Nullable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzchz {
    public static Looper zzaxp() {
        zzbq.zza(Looper.myLooper() != null, "Can't create handler inside thread that has not called Looper.prepare()");
        return Looper.myLooper();
    }

    public static Looper zzb(@Nullable Looper looper) {
        return looper != null ? looper : zzaxp();
    }
}
