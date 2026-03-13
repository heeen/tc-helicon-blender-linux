package com.google.android.gms.common;

import com.google.android.gms.common.internal.Hide;
import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes.dex */
@Hide
abstract class zzj extends zzh {
    private static final WeakReference<byte[]> zzfrg = new WeakReference<>(null);
    private WeakReference<byte[]> zzfrf;

    zzj(byte[] bArr) {
        super(bArr);
        this.zzfrf = zzfrg;
    }

    @Override // com.google.android.gms.common.zzh
    final byte[] getBytes() {
        byte[] bArrZzahi;
        synchronized (this) {
            bArrZzahi = this.zzfrf.get();
            if (bArrZzahi == null) {
                bArrZzahi = zzahi();
                this.zzfrf = new WeakReference<>(bArrZzahi);
            }
        }
        return bArrZzahi;
    }

    protected abstract byte[] zzahi();
}
