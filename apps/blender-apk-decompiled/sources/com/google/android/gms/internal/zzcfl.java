package com.google.android.gms.internal;

import android.app.PendingIntent;
import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
final class zzcfl extends zzcfp {
    private /* synthetic */ long zzitg;
    private /* synthetic */ PendingIntent zzith;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcfl(zzcfk zzcfkVar, GoogleApiClient googleApiClient, long j, PendingIntent pendingIntent) {
        super(googleApiClient);
        this.zzitg = j;
        this.zzith = pendingIntent;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zza(this.zzitg, this.zzith);
        setResult(Status.zzftq);
    }
}
