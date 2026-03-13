package com.google.android.gms.internal;

import android.app.PendingIntent;
import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.GeofencingRequest;

/* JADX INFO: loaded from: classes.dex */
final class zzcgo extends zzcgq {
    private /* synthetic */ PendingIntent zzhmu;
    private /* synthetic */ GeofencingRequest zziua;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcgo(zzcgn zzcgnVar, GoogleApiClient googleApiClient, GeofencingRequest geofencingRequest, PendingIntent pendingIntent) {
        super(googleApiClient);
        this.zziua = geofencingRequest;
        this.zzhmu = pendingIntent;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zza(this.zziua, this.zzhmu, this);
    }
}
