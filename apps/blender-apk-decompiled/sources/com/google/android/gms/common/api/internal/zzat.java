package com.google.android.gms.common.api.internal;

import com.google.android.gms.common.ConnectionResult;

/* JADX INFO: loaded from: classes.dex */
final class zzat extends zzbj {
    private /* synthetic */ com.google.android.gms.common.internal.zzj zzfxy;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzat(zzar zzarVar, zzbh zzbhVar, com.google.android.gms.common.internal.zzj zzjVar) {
        super(zzbhVar);
        this.zzfxy = zzjVar;
    }

    @Override // com.google.android.gms.common.api.internal.zzbj
    public final void zzajj() {
        this.zzfxy.zzf(new ConnectionResult(16, null));
    }
}
