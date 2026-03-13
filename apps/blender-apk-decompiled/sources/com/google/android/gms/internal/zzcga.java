package com.google.android.gms.internal;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.internal.zzcm;
import com.google.android.gms.location.LocationCallback;

/* JADX INFO: loaded from: classes.dex */
final class zzcga extends zzcgj {
    private /* synthetic */ LocationCallback zzitv;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcga(zzcfy zzcfyVar, GoogleApiClient googleApiClient, LocationCallback locationCallback) {
        super(googleApiClient);
        this.zzitv = locationCallback;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zzb(zzcm.zzb(this.zzitv, LocationCallback.class.getSimpleName()), new zzcgk(this));
    }
}
