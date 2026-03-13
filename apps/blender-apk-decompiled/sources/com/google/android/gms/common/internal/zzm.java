package com.google.android.gms.common.internal;

import android.support.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;

/* JADX INFO: loaded from: classes.dex */
public final class zzm implements zzj {
    private /* synthetic */ zzd zzgfk;

    public zzm(zzd zzdVar) {
        this.zzgfk = zzdVar;
    }

    @Override // com.google.android.gms.common.internal.zzj
    public final void zzf(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.isSuccess()) {
            this.zzgfk.zza((zzan) null, this.zzgfk.zzaly());
        } else if (this.zzgfk.zzgfc != null) {
            this.zzgfk.zzgfc.onConnectionFailed(connectionResult);
        }
    }
}
