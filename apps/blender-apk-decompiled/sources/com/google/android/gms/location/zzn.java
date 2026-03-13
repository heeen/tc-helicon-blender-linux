package com.google.android.gms.location;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.internal.zzci;
import com.google.android.gms.common.api.internal.zzcq;
import com.google.android.gms.internal.zzchh;
import com.google.android.gms.internal.zzchl;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: loaded from: classes.dex */
final class zzn extends zzcq<zzchh, LocationCallback> {
    private /* synthetic */ zzci zzhsp;
    private /* synthetic */ zzchl zzirm;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzn(FusedLocationProviderClient fusedLocationProviderClient, zzci zzciVar, zzchl zzchlVar, zzci zzciVar2) {
        super(zzciVar);
        this.zzirm = zzchlVar;
        this.zzhsp = zzciVar2;
    }

    @Override // com.google.android.gms.common.api.internal.zzcq
    protected final /* synthetic */ void zzb(Api.zzb zzbVar, TaskCompletionSource taskCompletionSource) throws RemoteException {
        ((zzchh) zzbVar).zza(this.zzirm, this.zzhsp, new FusedLocationProviderClient.zza(taskCompletionSource));
    }
}
