package com.google.android.gms.internal;

import android.os.Looper;
import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.internal.zzcm;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

/* JADX INFO: loaded from: classes.dex */
final class zzcgf extends zzcgj {
    private /* synthetic */ LocationRequest zzitt;
    private /* synthetic */ LocationCallback zzitv;
    private /* synthetic */ Looper zzity;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcgf(zzcfy zzcfyVar, GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationCallback locationCallback, Looper looper) {
        super(googleApiClient);
        this.zzitt = locationRequest;
        this.zzitv = locationCallback;
        this.zzity = looper;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zza(zzchl.zza(this.zzitt), zzcm.zzb(this.zzitv, zzchz.zzb(this.zzity), LocationCallback.class.getSimpleName()), new zzcgk(this));
    }
}
