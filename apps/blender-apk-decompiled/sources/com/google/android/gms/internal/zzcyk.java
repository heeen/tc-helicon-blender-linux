package com.google.android.gms.internal;

import android.support.annotation.Nullable;
import com.google.android.gms.common.api.Api;

/* JADX INFO: loaded from: classes.dex */
public final class zzcyk implements Api.ApiOptions.Optional {
    public static final zzcyk zzklp;
    private final boolean zzklq = false;
    private final boolean zzela = false;
    private final String zzelb = null;
    private final boolean zzenc = false;
    private final boolean zzklr = false;
    private final String zzend = null;
    private final Long zzkls = null;
    private final Long zzklt = null;

    static {
        new zzcyl();
        zzklp = new zzcyk(false, false, null, false, null, false, null, null);
    }

    private zzcyk(boolean z, boolean z2, String str, boolean z3, String str2, boolean z4, Long l, Long l2) {
    }

    public final String getServerClientId() {
        return this.zzelb;
    }

    public final boolean isIdTokenRequested() {
        return this.zzela;
    }

    public final boolean zzbeu() {
        return this.zzklq;
    }

    public final boolean zzbev() {
        return this.zzenc;
    }

    @Nullable
    public final String zzbew() {
        return this.zzend;
    }

    public final boolean zzbex() {
        return this.zzklr;
    }

    @Nullable
    public final Long zzbey() {
        return this.zzkls;
    }

    @Nullable
    public final Long zzbez() {
        return this.zzklt;
    }
}
