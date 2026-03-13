package com.google.android.gms.common.api.internal;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Api.ApiOptions;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class zzh<O extends Api.ApiOptions> {
    private final Api<O> zzfop;
    private final O zzfsm;
    private final boolean zzfud;
    private final int zzfue;

    private zzh(Api<O> api) {
        this.zzfud = true;
        this.zzfop = api;
        this.zzfsm = null;
        this.zzfue = System.identityHashCode(this);
    }

    private zzh(Api<O> api, O o) {
        this.zzfud = false;
        this.zzfop = api;
        this.zzfsm = o;
        this.zzfue = Arrays.hashCode(new Object[]{this.zzfop, this.zzfsm});
    }

    public static <O extends Api.ApiOptions> zzh<O> zza(Api<O> api, O o) {
        return new zzh<>(api, o);
    }

    public static <O extends Api.ApiOptions> zzh<O> zzb(Api<O> api) {
        return new zzh<>(api);
    }

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof zzh)) {
            return false;
        }
        zzh zzhVar = (zzh) obj;
        return !this.zzfud && !zzhVar.zzfud && com.google.android.gms.common.internal.zzbg.equal(this.zzfop, zzhVar.zzfop) && com.google.android.gms.common.internal.zzbg.equal(this.zzfsm, zzhVar.zzfsm);
    }

    public final int hashCode() {
        return this.zzfue;
    }

    public final String zzaig() {
        return this.zzfop.getName();
    }
}
