package com.google.android.gms.internal;

import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbhg {
    private static zzbhi zzgih;

    @Hide
    public static synchronized zzbhi zzanc() {
        if (zzgih == null) {
            zzgih = new zzbhh();
        }
        return zzgih;
    }
}
