package com.google.android.gms.common.api.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzbn implements zzl {
    private /* synthetic */ zzbm zzfzq;

    zzbn(zzbm zzbmVar) {
        this.zzfzq = zzbmVar;
    }

    @Override // com.google.android.gms.common.api.internal.zzl
    public final void zzbj(boolean z) {
        this.zzfzq.mHandler.sendMessage(this.zzfzq.mHandler.obtainMessage(1, Boolean.valueOf(z)));
    }
}
