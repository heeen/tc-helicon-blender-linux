package com.google.android.gms.common.api.internal;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Api.zzb;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzde<A extends Api.zzb, TResult> {
    @Hide
    protected abstract void zza(A a, TaskCompletionSource<TResult> taskCompletionSource) throws RemoteException;
}
