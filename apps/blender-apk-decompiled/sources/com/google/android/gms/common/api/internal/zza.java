package com.google.android.gms.common.api.internal;

import android.os.DeadObjectException;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.support.annotation.NonNull;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
public abstract class zza {
    private int zzenu;

    public zza(int i) {
        this.zzenu = i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Status zza(RemoteException remoteException) {
        StringBuilder sb = new StringBuilder();
        if (com.google.android.gms.common.util.zzs.zzanr() && (remoteException instanceof TransactionTooLargeException)) {
            sb.append("TransactionTooLargeException: ");
        }
        sb.append(remoteException.getLocalizedMessage());
        return new Status(8, sb.toString());
    }

    public abstract void zza(@NonNull zzae zzaeVar, boolean z);

    public abstract void zza(zzbo<?> zzboVar) throws DeadObjectException;

    public abstract void zzs(@NonNull Status status);
}
