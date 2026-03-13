package com.google.android.gms.internal;

import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
final class zzcgk extends zzcgs {
    private final com.google.android.gms.common.api.internal.zzn<Status> zzgbf;

    public zzcgk(com.google.android.gms.common.api.internal.zzn<Status> zznVar) {
        this.zzgbf = zznVar;
    }

    @Override // com.google.android.gms.internal.zzcgr
    public final void zza(zzcgl zzcglVar) {
        this.zzgbf.setResult(zzcglVar.getStatus());
    }
}
