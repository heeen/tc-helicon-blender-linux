package com.google.android.gms.internal;

import android.os.DeadObjectException;
import android.os.IInterface;

/* JADX INFO: loaded from: classes.dex */
final class zzcfr implements zzchr<zzcgw> {
    private /* synthetic */ zzcfq zzitl;

    zzcfr(zzcfq zzcfqVar) {
        this.zzitl = zzcfqVar;
    }

    @Override // com.google.android.gms.internal.zzchr
    public final void zzalv() {
        this.zzitl.zzalv();
    }

    @Override // com.google.android.gms.internal.zzchr
    public final /* synthetic */ IInterface zzalw() throws DeadObjectException {
        return (zzcgw) this.zzitl.zzalw();
    }
}
