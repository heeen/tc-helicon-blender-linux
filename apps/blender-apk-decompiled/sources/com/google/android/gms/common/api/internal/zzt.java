package com.google.android.gms.common.api.internal;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;

/* JADX INFO: loaded from: classes.dex */
public final class zzt implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public final Api<?> zzfop;
    private final boolean zzfvo;
    private zzu zzfvp;

    public zzt(Api<?> api, boolean z) {
        this.zzfop = api;
        this.zzfvo = z;
    }

    private final void zzair() {
        com.google.android.gms.common.internal.zzbq.checkNotNull(this.zzfvp, "Callbacks must be attached to a ClientConnectionHelper instance before connecting the client.");
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
    public final void onConnected(@Nullable Bundle bundle) {
        zzair();
        this.zzfvp.onConnected(bundle);
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
    public final void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        zzair();
        this.zzfvp.zza(connectionResult, this.zzfop, this.zzfvo);
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
    public final void onConnectionSuspended(int i) {
        zzair();
        this.zzfvp.onConnectionSuspended(i);
    }

    public final void zza(zzu zzuVar) {
        this.zzfvp = zzuVar;
    }
}
