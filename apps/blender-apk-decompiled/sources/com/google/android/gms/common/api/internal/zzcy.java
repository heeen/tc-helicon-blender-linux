package com.google.android.gms.common.api.internal;

import android.support.annotation.WorkerThread;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Scope;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
@WorkerThread
public interface zzcy {
    void zzb(com.google.android.gms.common.internal.zzan zzanVar, Set<Scope> set);

    void zzh(ConnectionResult connectionResult);
}
