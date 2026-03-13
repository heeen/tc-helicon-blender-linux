package com.google.android.gms.internal;

import android.app.PendingIntent;
import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;

/* JADX INFO: loaded from: classes.dex */
final class zzcfo extends zzcfp {
    private /* synthetic */ PendingIntent zzhmu;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcfo(zzcfk zzcfkVar, GoogleApiClient googleApiClient, PendingIntent pendingIntent) {
        super(googleApiClient);
        this.zzhmu = pendingIntent;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zza(this.zzhmu, this);
    }
}
