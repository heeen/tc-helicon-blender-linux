package com.google.android.gms.common.api.internal;

import android.os.Bundle;
import android.support.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

/* JADX INFO: loaded from: classes.dex */
final class zzax implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private /* synthetic */ zzao zzfxt;

    private zzax(zzao zzaoVar) {
        this.zzfxt = zzaoVar;
    }

    /* synthetic */ zzax(zzao zzaoVar, zzap zzapVar) {
        this(zzaoVar);
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
    public final void onConnected(Bundle bundle) {
        this.zzfxt.zzfxl.zza(new zzav(this.zzfxt));
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
    public final void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        this.zzfxt.zzfwa.lock();
        try {
            if (this.zzfxt.zzd(connectionResult)) {
                this.zzfxt.zzajn();
                this.zzfxt.zzajl();
            } else {
                this.zzfxt.zze(connectionResult);
            }
        } finally {
            this.zzfxt.zzfwa.unlock();
        }
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
    public final void onConnectionSuspended(int i) {
    }
}
