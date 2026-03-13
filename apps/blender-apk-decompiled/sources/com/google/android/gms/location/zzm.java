package com.google.android.gms.location;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.internal.zzde;
import com.google.android.gms.internal.zzchh;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: loaded from: classes.dex */
final class zzm extends zzde<zzchh, LocationAvailability> {
    zzm(FusedLocationProviderClient fusedLocationProviderClient) {
    }

    @Override // com.google.android.gms.common.api.internal.zzde
    protected final /* synthetic */ void zza(Api.zzb zzbVar, TaskCompletionSource<LocationAvailability> taskCompletionSource) throws RemoteException {
        taskCompletionSource.setResult(((zzchh) zzbVar).zzaxb());
    }
}
