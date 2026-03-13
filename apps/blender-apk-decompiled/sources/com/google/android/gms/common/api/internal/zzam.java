package com.google.android.gms.common.api.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzam extends zzbj {
    private /* synthetic */ zzal zzfxf;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzam(zzal zzalVar, zzbh zzbhVar) {
        super(zzbhVar);
        this.zzfxf = zzalVar;
    }

    @Override // com.google.android.gms.common.api.internal.zzbj
    public final void zzajj() {
        this.zzfxf.onConnectionSuspended(1);
    }
}
