package com.google.android.gms.common.api.internal;

import android.support.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
final class zzbd implements GoogleApiClient.OnConnectionFailedListener {
    private /* synthetic */ zzdb zzfyt;

    zzbd(zzba zzbaVar, zzdb zzdbVar) {
        this.zzfyt = zzdbVar;
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
    public final void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        this.zzfyt.setResult(new Status(8));
    }
}
