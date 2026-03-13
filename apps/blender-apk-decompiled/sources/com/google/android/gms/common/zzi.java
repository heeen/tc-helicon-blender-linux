package com.google.android.gms.common;

import com.google.android.gms.common.internal.Hide;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
@Hide
final class zzi extends zzh {
    private final byte[] zzfre;

    zzi(byte[] bArr) {
        super(Arrays.copyOfRange(bArr, 0, 25));
        this.zzfre = bArr;
    }

    @Override // com.google.android.gms.common.zzh
    final byte[] getBytes() {
        return this.zzfre;
    }
}
