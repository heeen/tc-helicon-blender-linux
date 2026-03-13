package com.google.android.gms.internal;

import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzccp {
    private static zzccp zzhqf;
    private final zzcck zzhqg = new zzcck();
    private final zzccl zzhqh = new zzccl();

    static {
        zzccp zzccpVar = new zzccp();
        synchronized (zzccp.class) {
            zzhqf = zzccpVar;
        }
    }

    private zzccp() {
    }

    private static zzccp zzasm() {
        zzccp zzccpVar;
        synchronized (zzccp.class) {
            zzccpVar = zzhqf;
        }
        return zzccpVar;
    }

    public static zzcck zzasn() {
        return zzasm().zzhqg;
    }

    public static zzccl zzaso() {
        return zzasm().zzhqh;
    }
}
