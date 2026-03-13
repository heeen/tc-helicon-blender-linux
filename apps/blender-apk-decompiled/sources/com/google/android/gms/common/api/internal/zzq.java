package com.google.android.gms.common.api.internal;

import android.support.annotation.MainThread;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiActivity;

/* JADX INFO: loaded from: classes.dex */
final class zzq implements Runnable {
    private final zzp zzfux;
    final /* synthetic */ zzo zzfuy;

    zzq(zzo zzoVar, zzp zzpVar) {
        this.zzfuy = zzoVar;
        this.zzfux = zzpVar;
    }

    @Override // java.lang.Runnable
    @MainThread
    public final void run() {
        if (this.zzfuy.mStarted) {
            ConnectionResult connectionResultZzain = this.zzfux.zzain();
            if (connectionResultZzain.hasResolution()) {
                this.zzfuy.zzgam.startActivityForResult(GoogleApiActivity.zza(this.zzfuy.getActivity(), connectionResultZzain.getResolution(), this.zzfux.zzaim(), false), 1);
                return;
            }
            if (this.zzfuy.zzftg.isUserResolvableError(connectionResultZzain.getErrorCode())) {
                this.zzfuy.zzftg.zza(this.zzfuy.getActivity(), this.zzfuy.zzgam, connectionResultZzain.getErrorCode(), 2, this.zzfuy);
            } else if (connectionResultZzain.getErrorCode() != 18) {
                this.zzfuy.zza(connectionResultZzain, this.zzfux.zzaim());
            } else {
                GoogleApiAvailability.zza(this.zzfuy.getActivity().getApplicationContext(), new zzr(this, GoogleApiAvailability.zza(this.zzfuy.getActivity(), this.zzfuy)));
            }
        }
    }
}
