package com.google.android.gms.internal;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;

/* JADX INFO: loaded from: classes.dex */
final class zzcgp extends zzcgq {
    private /* synthetic */ com.google.android.gms.location.zzal zziub;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcgp(zzcgn zzcgnVar, GoogleApiClient googleApiClient, com.google.android.gms.location.zzal zzalVar) {
        super(googleApiClient);
        this.zziub = zzalVar;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zza(this.zziub, this);
    }
}
