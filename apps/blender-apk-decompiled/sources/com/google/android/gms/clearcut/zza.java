package com.google.android.gms.clearcut;

import android.content.Context;
import android.os.Looper;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.internal.zzr;
import com.google.android.gms.internal.zzbfn;

/* JADX INFO: loaded from: classes.dex */
final class zza extends Api.zza<zzbfn, Api.ApiOptions.NoOptions> {
    zza() {
    }

    @Override // com.google.android.gms.common.api.Api.zza
    public final /* synthetic */ Api.zze zza(Context context, Looper looper, zzr zzrVar, Api.ApiOptions.NoOptions noOptions, GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        return new zzbfn(context, looper, zzrVar, connectionCallbacks, onConnectionFailedListener);
    }
}
