package com.google.android.gms.common.api.internal;

import com.google.android.gms.common.ConnectionResult;

/* JADX INFO: loaded from: classes.dex */
final class zzcw implements Runnable {
    private /* synthetic */ zzcv zzgbe;

    zzcw(zzcv zzcvVar) {
        this.zzgbe = zzcvVar;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.zzgbe.zzgbd.zzh(new ConnectionResult(4));
    }
}
