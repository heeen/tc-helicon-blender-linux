package com.google.android.gms.internal;

import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
final class zzfgu implements zzfgw {
    private zzfgu() {
    }

    /* synthetic */ zzfgu(zzfgt zzfgtVar) {
        this();
    }

    @Override // com.google.android.gms.internal.zzfgw
    public final byte[] zzg(byte[] bArr, int i, int i2) {
        return Arrays.copyOfRange(bArr, i, i2 + i);
    }
}
