package com.google.android.gms.common.api;

import com.google.android.gms.common.api.PendingResult;

/* JADX INFO: loaded from: classes.dex */
final class zza implements PendingResult.zza {
    private /* synthetic */ Batch zzfsj;

    zza(Batch batch) {
        this.zzfsj = batch;
    }

    @Override // com.google.android.gms.common.api.PendingResult.zza
    public final void zzr(Status status) {
        synchronized (this.zzfsj.mLock) {
            if (this.zzfsj.isCanceled()) {
                return;
            }
            if (status.isCanceled()) {
                Batch.zza(this.zzfsj, true);
            } else if (!status.isSuccess()) {
                Batch.zzb(this.zzfsj, true);
            }
            Batch.zzb(this.zzfsj);
            if (this.zzfsj.zzfsf == 0) {
                if (this.zzfsj.zzfsh) {
                    super/*com.google.android.gms.common.api.internal.BasePendingResult*/.cancel();
                } else {
                    this.zzfsj.setResult(new BatchResult(this.zzfsj.zzfsg ? new Status(13) : Status.zzftq, this.zzfsj.zzfsi));
                }
            }
        }
    }
}
