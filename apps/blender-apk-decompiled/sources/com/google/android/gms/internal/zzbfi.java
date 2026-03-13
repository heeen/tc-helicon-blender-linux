package com.google.android.gms.internal;

import android.content.Context;
import android.support.annotation.NonNull;
import com.google.android.gms.clearcut.ClearcutLogger;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
public final class zzbfi extends GoogleApi<Api.ApiOptions.NoOptions> implements com.google.android.gms.clearcut.zzb {
    private zzbfi(@NonNull Context context) {
        super(context, ClearcutLogger.API, (Api.ApiOptions) null, new com.google.android.gms.common.api.internal.zzg());
    }

    public static com.google.android.gms.clearcut.zzb zzcb(@NonNull Context context) {
        return new zzbfi(context);
    }

    @Override // com.google.android.gms.clearcut.zzb
    public final PendingResult<Status> zza(com.google.android.gms.clearcut.zze zzeVar) {
        return zzc(new zzbfl(zzeVar, zzahw()));
    }
}
