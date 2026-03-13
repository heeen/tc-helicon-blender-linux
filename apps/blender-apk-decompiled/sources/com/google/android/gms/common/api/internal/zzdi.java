package com.google.android.gms.common.api.internal;

import android.support.annotation.WorkerThread;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;

/* JADX INFO: loaded from: classes.dex */
final class zzdi implements Runnable {
    private /* synthetic */ Result zzgbo;
    private /* synthetic */ zzdh zzgbp;

    zzdi(zzdh zzdhVar, Result result) {
        this.zzgbp = zzdhVar;
        this.zzgbo = result;
    }

    @Override // java.lang.Runnable
    @WorkerThread
    public final void run() {
        try {
            try {
                BasePendingResult.zzfvb.set(true);
                this.zzgbp.zzgbm.sendMessage(this.zzgbp.zzgbm.obtainMessage(0, this.zzgbp.zzgbh.onSuccess(this.zzgbo)));
                BasePendingResult.zzfvb.set(false);
                zzdh zzdhVar = this.zzgbp;
                zzdh.zzd(this.zzgbo);
                GoogleApiClient googleApiClient = (GoogleApiClient) this.zzgbp.zzfve.get();
                if (googleApiClient != null) {
                    googleApiClient.zzb(this.zzgbp);
                }
            } catch (RuntimeException e) {
                this.zzgbp.zzgbm.sendMessage(this.zzgbp.zzgbm.obtainMessage(1, e));
                BasePendingResult.zzfvb.set(false);
                zzdh zzdhVar2 = this.zzgbp;
                zzdh.zzd(this.zzgbo);
                GoogleApiClient googleApiClient2 = (GoogleApiClient) this.zzgbp.zzfve.get();
                if (googleApiClient2 != null) {
                    googleApiClient2.zzb(this.zzgbp);
                }
            }
        } catch (Throwable th) {
            BasePendingResult.zzfvb.set(false);
            zzdh zzdhVar3 = this.zzgbp;
            zzdh.zzd(this.zzgbo);
            GoogleApiClient googleApiClient3 = (GoogleApiClient) this.zzgbp.zzfve.get();
            if (googleApiClient3 != null) {
                googleApiClient3.zzb(this.zzgbp);
            }
            throw th;
        }
    }
}
