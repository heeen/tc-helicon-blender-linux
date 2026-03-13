package com.google.android.gms.common.api.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzbt implements Runnable {
    private /* synthetic */ zzbs zzgac;

    zzbt(zzbs zzbsVar) {
        this.zzgac = zzbsVar;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.zzgac.zzgaa.zzfwd.disconnect();
    }
}
