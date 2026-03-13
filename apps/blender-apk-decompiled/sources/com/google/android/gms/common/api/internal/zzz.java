package com.google.android.gms.common.api.internal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Api.ApiOptions;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzcyj;
import com.google.android.gms.internal.zzcyk;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzz<O extends Api.ApiOptions> extends GoogleApi<O> {
    private final Api.zza<? extends zzcyj, zzcyk> zzfth;
    private final Api.zze zzfwd;
    private final zzt zzfwe;
    private final com.google.android.gms.common.internal.zzr zzfwf;

    public zzz(@NonNull Context context, Api<O> api, Looper looper, @NonNull Api.zze zzeVar, @NonNull zzt zztVar, com.google.android.gms.common.internal.zzr zzrVar, Api.zza<? extends zzcyj, zzcyk> zzaVar) {
        super(context, api, looper);
        this.zzfwd = zzeVar;
        this.zzfwe = zztVar;
        this.zzfwf = zzrVar;
        this.zzfth = zzaVar;
        this.zzfsq.zza(this);
    }

    @Override // com.google.android.gms.common.api.GoogleApi
    public final Api.zze zza(Looper looper, zzbo<O> zzboVar) {
        this.zzfwe.zza(zzboVar);
        return this.zzfwd;
    }

    @Override // com.google.android.gms.common.api.GoogleApi
    public final zzcv zza(Context context, Handler handler) {
        return new zzcv(context, handler, this.zzfwf, this.zzfth);
    }

    public final Api.zze zzaix() {
        return this.zzfwd;
    }
}
