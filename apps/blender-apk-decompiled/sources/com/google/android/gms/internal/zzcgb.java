package com.google.android.gms.internal;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
final class zzcgb extends zzcgj {
    private /* synthetic */ boolean zzitw;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcgb(zzcfy zzcfyVar, GoogleApiClient googleApiClient, boolean z) {
        super(googleApiClient);
        this.zzitw = z;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zzbo(this.zzitw);
        setResult(Status.zzftq);
    }
}
