package com.google.android.gms.common.api.internal;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Api.zzb;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzdo<A extends Api.zzb, L> {
    private final zzck<L> zzgau;

    protected zzdo(zzck<L> zzckVar) {
        this.zzgau = zzckVar;
    }

    @Hide
    public final zzck<L> zzakx() {
        return this.zzgau;
    }

    @Hide
    protected abstract void zzc(A a, TaskCompletionSource<Boolean> taskCompletionSource) throws RemoteException;
}
