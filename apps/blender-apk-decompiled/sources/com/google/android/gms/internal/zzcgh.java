package com.google.android.gms.internal;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.internal.zzcm;
import com.google.android.gms.location.LocationListener;

/* JADX INFO: loaded from: classes.dex */
final class zzcgh extends zzcgj {
    private /* synthetic */ LocationListener zzitu;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcgh(zzcfy zzcfyVar, GoogleApiClient googleApiClient, LocationListener locationListener) {
        super(googleApiClient);
        this.zzitu = locationListener;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zza(zzcm.zzb(this.zzitu, LocationListener.class.getSimpleName()), new zzcgk(this));
    }
}
