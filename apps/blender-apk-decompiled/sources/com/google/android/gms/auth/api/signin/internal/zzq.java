package com.google.android.gms.auth.api.signin.internal;

/* JADX INFO: loaded from: classes.dex */
public final class zzq {
    private static int zzenz = 31;
    private int zzeoa = 1;

    public final int zzacr() {
        return this.zzeoa;
    }

    public final zzq zzav(boolean z) {
        this.zzeoa = (zzenz * this.zzeoa) + (z ? 1 : 0);
        return this;
    }

    public final zzq zzs(Object obj) {
        this.zzeoa = (zzenz * this.zzeoa) + (obj == null ? 0 : obj.hashCode());
        return this;
    }
}
