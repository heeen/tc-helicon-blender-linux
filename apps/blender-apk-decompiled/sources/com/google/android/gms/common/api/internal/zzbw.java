package com.google.android.gms.common.api.internal;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Api.ApiOptions;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.Result;

/* JADX INFO: loaded from: classes.dex */
public final class zzbw<O extends Api.ApiOptions> extends zzak {
    private final GoogleApi<O> zzgaf;

    public zzbw(GoogleApi<O> googleApi) {
        super("Method is not supported by connectionless client. APIs supporting connectionless client must not call this method.");
        this.zzgaf = googleApi;
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient
    public final Context getContext() {
        return this.zzgaf.getApplicationContext();
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient
    public final Looper getLooper() {
        return this.zzgaf.getLooper();
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient
    public final void zza(zzdh zzdhVar) {
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient
    public final void zzb(zzdh zzdhVar) {
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient
    public final <A extends Api.zzb, R extends Result, T extends zzm<R, A>> T zzd(@NonNull T t) {
        return (T) this.zzgaf.zza(t);
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient
    public final <A extends Api.zzb, T extends zzm<? extends Result, A>> T zze(@NonNull T t) {
        return (T) this.zzgaf.zzb(t);
    }
}
