package com.google.android.gms.common.api.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzbq implements Runnable {
    private /* synthetic */ zzbo zzgaa;

    zzbq(zzbo zzboVar) {
        this.zzgaa = zzboVar;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.zzgaa.zzakg();
    }
}
