package com.google.android.gms.common.api.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzap implements Runnable {
    private /* synthetic */ zzao zzfxt;

    zzap(zzao zzaoVar) {
        this.zzfxt = zzaoVar;
    }

    @Override // java.lang.Runnable
    public final void run() {
        com.google.android.gms.common.zzf.zzcf(this.zzfxt.mContext);
    }
}
