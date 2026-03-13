package com.google.android.gms.internal;

import android.content.Context;
import android.os.Looper;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;

/* JADX INFO: loaded from: classes.dex */
final class zzcyh extends Api.zza<zzcyt, zzcyk> {
    zzcyh() {
    }

    @Override // com.google.android.gms.common.api.Api.zza
    public final /* synthetic */ Api.zze zza(Context context, Looper looper, com.google.android.gms.common.internal.zzr zzrVar, zzcyk zzcykVar, GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        zzcyk zzcykVar2 = zzcykVar;
        if (zzcykVar2 == null) {
            zzcykVar2 = zzcyk.zzklp;
        }
        return new zzcyt(context, looper, true, zzrVar, zzcykVar2, connectionCallbacks, onConnectionFailedListener);
    }
}
