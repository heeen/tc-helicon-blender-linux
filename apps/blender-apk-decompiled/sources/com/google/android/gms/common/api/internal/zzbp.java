package com.google.android.gms.common.api.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzbp implements Runnable {
    private /* synthetic */ zzbo zzgaa;

    zzbp(zzbo zzboVar) {
        this.zzgaa = zzboVar;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.zzgaa.zzakf();
    }
}
