package com.google.android.gms.internal;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;

/* JADX INFO: loaded from: classes.dex */
abstract class zzcgj extends LocationServices.zza<Status> {
    public zzcgj(GoogleApiClient googleApiClient) {
        super(googleApiClient);
    }

    @Override // com.google.android.gms.common.api.internal.BasePendingResult
    public final /* synthetic */ Result zzb(Status status) {
        return status;
    }
}
