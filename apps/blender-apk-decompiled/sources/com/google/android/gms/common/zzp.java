package com.google.android.gms.common;

import android.support.annotation.NonNull;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
class zzp {
    private static final zzp zzfrl = new zzp(true, null, null);
    final Throwable cause;
    final boolean zzfrm;
    private String zzfrn;

    zzp(boolean z, String str, Throwable th) {
        this.zzfrm = z;
        this.zzfrn = str;
        this.cause = th;
    }

    static zzp zza(String str, zzh zzhVar, boolean z, boolean z2) {
        return new zzr(str, zzhVar, z, z2);
    }

    static zzp zzahj() {
        return zzfrl;
    }

    static zzp zzd(@NonNull String str, @NonNull Throwable th) {
        return new zzp(false, str, th);
    }

    static zzp zzgg(@NonNull String str) {
        return new zzp(false, str, null);
    }

    String getErrorMessage() {
        return this.zzfrn;
    }
}
