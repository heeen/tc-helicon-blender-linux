package com.google.android.gms.location;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.internal.zzck;
import com.google.android.gms.common.api.internal.zzdo;
import com.google.android.gms.internal.zzchh;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: loaded from: classes.dex */
final class zzo extends zzdo<zzchh, LocationCallback> {
    private /* synthetic */ FusedLocationProviderClient zzirn;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    zzo(FusedLocationProviderClient fusedLocationProviderClient, zzck zzckVar) {
        super(zzckVar);
        this.zzirn = fusedLocationProviderClient;
    }

    @Override // com.google.android.gms.common.api.internal.zzdo
    protected final /* synthetic */ void zzc(Api.zzb zzbVar, TaskCompletionSource taskCompletionSource) throws RemoteException {
        try {
            ((zzchh) zzbVar).zzb(zzakx(), this.zzirn.zzc((TaskCompletionSource<Boolean>) taskCompletionSource));
        } catch (RuntimeException e) {
            taskCompletionSource.trySetException(e);
        }
    }
}
