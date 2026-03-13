package com.google.android.gms.internal;

import android.os.RemoteException;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
final class zzbgx extends zzbgr {
    private final com.google.android.gms.common.api.internal.zzn<Status> zzgbf;

    public zzbgx(com.google.android.gms.common.api.internal.zzn<Status> zznVar) {
        this.zzgbf = zznVar;
    }

    @Override // com.google.android.gms.internal.zzbgr, com.google.android.gms.internal.zzbhb
    public final void zzch(int i) throws RemoteException {
        this.zzgbf.setResult(new Status(i));
    }
}
