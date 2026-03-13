package com.google.android.gms.internal;

import android.app.PendingIntent;
import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
final class zzcfm extends zzcfp {
    private /* synthetic */ PendingIntent zzith;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcfm(zzcfk zzcfkVar, GoogleApiClient googleApiClient, PendingIntent pendingIntent) {
        super(googleApiClient);
        this.zzith = pendingIntent;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zzc(this.zzith);
        setResult(Status.zzftq);
    }
}
