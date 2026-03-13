package com.google.android.gms.internal;

import android.location.Location;
import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
final class zzcgc extends zzcgj {
    private /* synthetic */ Location zzitx;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcgc(zzcfy zzcfyVar, GoogleApiClient googleApiClient, Location location) {
        super(googleApiClient);
        this.zzitx = location;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zzc(this.zzitx);
        setResult(Status.zzftq);
    }
}
