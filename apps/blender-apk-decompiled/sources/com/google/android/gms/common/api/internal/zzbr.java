package com.google.android.gms.common.api.internal;

import com.google.android.gms.common.ConnectionResult;

/* JADX INFO: loaded from: classes.dex */
final class zzbr implements Runnable {
    private /* synthetic */ zzbo zzgaa;
    private /* synthetic */ ConnectionResult zzgab;

    zzbr(zzbo zzboVar, ConnectionResult connectionResult) {
        this.zzgaa = zzboVar;
        this.zzgab = connectionResult;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.zzgaa.onConnectionFailed(this.zzgab);
    }
}
