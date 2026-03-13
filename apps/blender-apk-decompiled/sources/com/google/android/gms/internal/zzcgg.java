package com.google.android.gms.internal;

import android.app.PendingIntent;
import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;

/* JADX INFO: loaded from: classes.dex */
final class zzcgg extends zzcgj {
    private /* synthetic */ PendingIntent zzith;
    private /* synthetic */ LocationRequest zzitt;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcgg(zzcfy zzcfyVar, GoogleApiClient googleApiClient, LocationRequest locationRequest, PendingIntent pendingIntent) {
        super(googleApiClient);
        this.zzitt = locationRequest;
        this.zzith = pendingIntent;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zza(this.zzitt, this.zzith, new zzcgk(this));
    }
}
