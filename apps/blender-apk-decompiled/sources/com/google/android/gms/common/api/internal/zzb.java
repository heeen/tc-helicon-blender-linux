package com.google.android.gms.common.api.internal;

import android.os.DeadObjectException;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: loaded from: classes.dex */
abstract class zzb<T> extends zza {
    protected final TaskCompletionSource<T> zzejm;

    public zzb(int i, TaskCompletionSource<T> taskCompletionSource) {
        super(i);
        this.zzejm = taskCompletionSource;
    }

    @Override // com.google.android.gms.common.api.internal.zza
    public void zza(@NonNull zzae zzaeVar, boolean z) {
    }

    @Override // com.google.android.gms.common.api.internal.zza
    public final void zza(zzbo<?> zzboVar) throws DeadObjectException {
        try {
            zzb(zzboVar);
        } catch (DeadObjectException e) {
            zzs(zza.zza(e));
            throw e;
        } catch (RemoteException e2) {
            zzs(zza.zza(e2));
        } catch (RuntimeException e3) {
            zza(e3);
        }
    }

    public void zza(@NonNull RuntimeException runtimeException) {
        this.zzejm.trySetException(runtimeException);
    }

    protected abstract void zzb(zzbo<?> zzboVar) throws RemoteException;

    @Override // com.google.android.gms.common.api.internal.zza
    public void zzs(@NonNull Status status) {
        this.zzejm.trySetException(new ApiException(status));
    }
}
