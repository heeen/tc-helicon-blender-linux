package com.google.android.gms.common.api.internal;

import android.os.DeadObjectException;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: loaded from: classes.dex */
public final class zze<TResult> extends zza {
    private final TaskCompletionSource<TResult> zzejm;
    private final zzde<Api.zzb, TResult> zzfua;
    private final zzda zzfub;

    public zze(int i, zzde<Api.zzb, TResult> zzdeVar, TaskCompletionSource<TResult> taskCompletionSource, zzda zzdaVar) {
        super(i);
        this.zzejm = taskCompletionSource;
        this.zzfua = zzdeVar;
        this.zzfub = zzdaVar;
    }

    @Override // com.google.android.gms.common.api.internal.zza
    public final void zza(@NonNull zzae zzaeVar, boolean z) {
        zzaeVar.zza(this.zzejm, z);
    }

    @Override // com.google.android.gms.common.api.internal.zza
    public final void zza(zzbo<?> zzboVar) throws DeadObjectException {
        try {
            this.zzfua.zza(zzboVar.zzaix(), this.zzejm);
        } catch (DeadObjectException e) {
            throw e;
        } catch (RemoteException e2) {
            zzs(zza.zza(e2));
        } catch (RuntimeException e3) {
            this.zzejm.trySetException(e3);
        }
    }

    @Override // com.google.android.gms.common.api.internal.zza
    public final void zzs(@NonNull Status status) {
        this.zzejm.trySetException(this.zzfub.zzt(status));
    }
}
