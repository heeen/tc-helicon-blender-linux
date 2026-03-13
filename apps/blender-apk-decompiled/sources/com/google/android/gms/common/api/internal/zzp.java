package com.google.android.gms.common.api.internal;

import com.google.android.gms.common.ConnectionResult;

/* JADX INFO: loaded from: classes.dex */
final class zzp {
    private final int zzfuv;
    private final ConnectionResult zzfuw;

    zzp(ConnectionResult connectionResult, int i) {
        com.google.android.gms.common.internal.zzbq.checkNotNull(connectionResult);
        this.zzfuw = connectionResult;
        this.zzfuv = i;
    }

    final int zzaim() {
        return this.zzfuv;
    }

    final ConnectionResult zzain() {
        return this.zzfuw;
    }
}
