package com.google.android.gms.internal;

import android.app.PendingIntent;
import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityTransitionRequest;

/* JADX INFO: loaded from: classes.dex */
final class zzcfn extends zzcfp {
    private /* synthetic */ PendingIntent zzhmu;
    private /* synthetic */ ActivityTransitionRequest zziti;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzcfn(zzcfk zzcfkVar, GoogleApiClient googleApiClient, ActivityTransitionRequest activityTransitionRequest, PendingIntent pendingIntent) {
        super(googleApiClient);
        this.zziti = activityTransitionRequest;
        this.zzhmu = pendingIntent;
    }

    @Override // com.google.android.gms.common.api.internal.zzm
    protected final /* synthetic */ void zza(Api.zzb zzbVar) throws RemoteException {
        ((zzchh) zzbVar).zza(this.zziti, this.zzhmu, this);
    }
}
