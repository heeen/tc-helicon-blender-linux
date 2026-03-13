package com.google.android.gms.internal;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.internal.zzcm;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/* JADX INFO: loaded from: classes.dex */
final class zzcfz extends zzcgj {
    private /* synthetic */ LocationRequest zzitt;
    private /* synthetic */ LocationListener zzitu;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcfz(zzcfy zzcfyVar, GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationListener locationListener) {
        super(googleApiClient);
        this.zzitt = locationRequest;
        this.zzitu = locationListener;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zza(this.zzitt, zzcm.zzb(this.zzitu, zzchz.zzaxp(), LocationListener.class.getSimpleName()), new zzcgk(this));
    }
}
