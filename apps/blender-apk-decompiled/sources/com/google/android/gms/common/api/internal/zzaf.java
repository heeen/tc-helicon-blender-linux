package com.google.android.gms.common.api.internal;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
final class zzaf implements PendingResult.zza {
    private /* synthetic */ BasePendingResult zzfwy;
    private /* synthetic */ zzae zzfwz;

    zzaf(zzae zzaeVar, BasePendingResult basePendingResult) {
        this.zzfwz = zzaeVar;
        this.zzfwy = basePendingResult;
    }

    @Override // com.google.android.gms.common.api.PendingResult.zza
    public final void zzr(Status status) {
        this.zzfwz.zzfww.remove(this.zzfwy);
    }
}
