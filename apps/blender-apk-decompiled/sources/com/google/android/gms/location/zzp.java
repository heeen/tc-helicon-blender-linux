package com.google.android.gms.location;

import android.os.RemoteException;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.internal.zzcgl;
import com.google.android.gms.internal.zzcgs;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: loaded from: classes.dex */
final class zzp extends zzcgs {
    private /* synthetic */ TaskCompletionSource zzeuo;

    zzp(FusedLocationProviderClient fusedLocationProviderClient, TaskCompletionSource taskCompletionSource) {
        this.zzeuo = taskCompletionSource;
    }

    @Override // com.google.android.gms.internal.zzcgr
    public final void zza(zzcgl zzcglVar) throws RemoteException {
        Status status = zzcglVar.getStatus();
        if (status == null) {
            this.zzeuo.trySetException(new ApiException(new Status(8, "Got null status from location service")));
        } else if (status.getStatusCode() == 0) {
            this.zzeuo.setResult(true);
        } else {
            this.zzeuo.trySetException(com.google.android.gms.common.internal.zzb.zzy(status));
        }
    }
}
