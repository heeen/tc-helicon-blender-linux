package com.google.android.gms.internal;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

/* JADX INFO: loaded from: classes.dex */
final class zzcht extends LocationServices.zza<LocationSettingsResult> {
    private /* synthetic */ LocationSettingsRequest zziuv;
    private /* synthetic */ String zziuw = null;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcht(zzchs zzchsVar, GoogleApiClient googleApiClient, LocationSettingsRequest locationSettingsRequest, String str) {
        super(googleApiClient);
        this.zziuv = locationSettingsRequest;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zza(this.zziuv, this, this.zziuw);
    }

    @Override // com.google.android.gms.common.api.internal.BasePendingResult
    public final /* synthetic */ Result zzb(Status status) {
        return new LocationSettingsResult(status);
    }
}
