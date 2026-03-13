package com.google.android.gms.internal;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
public final class zzbgv implements zzbgu {
    @Override // com.google.android.gms.internal.zzbgu
    public final PendingResult<Status> zzd(GoogleApiClient googleApiClient) {
        return googleApiClient.zze(new zzbgw(this, googleApiClient));
    }
}
