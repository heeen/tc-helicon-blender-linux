package com.google.android.gms.common.internal;

import android.support.annotation.NonNull;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzam {

    @NonNull
    private final String mPackageName;

    @NonNull
    private final String zzghk;
    private final boolean zzghl = false;
    private final int zzggv = 129;

    public zzam(@NonNull String str, @NonNull String str2, boolean z, int i) {
        this.mPackageName = str;
        this.zzghk = str2;
    }

    @NonNull
    final String getPackageName() {
        return this.mPackageName;
    }

    final int zzamu() {
        return this.zzggv;
    }

    @NonNull
    final String zzamx() {
        return this.zzghk;
    }
}
