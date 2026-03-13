package com.google.android.gms.common.internal;

import android.support.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

/* JADX INFO: loaded from: classes.dex */
final class zzad implements zzg {
    private /* synthetic */ GoogleApiClient.OnConnectionFailedListener zzggk;

    zzad(GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        this.zzggk = onConnectionFailedListener;
    }

    @Override // com.google.android.gms.common.internal.zzg
    public final void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        this.zzggk.onConnectionFailed(connectionResult);
    }
}
