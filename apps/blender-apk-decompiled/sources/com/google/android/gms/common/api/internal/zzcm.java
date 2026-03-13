package com.google.android.gms.common.api.internal;

import android.os.Looper;
import android.support.annotation.NonNull;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes.dex */
public final class zzcm {
    private final Set<zzci<?>> zzfgd = Collections.newSetFromMap(new WeakHashMap());

    public static <L> zzci<L> zzb(@NonNull L l, @NonNull Looper looper, @NonNull String str) {
        com.google.android.gms.common.internal.zzbq.checkNotNull(l, "Listener must not be null");
        com.google.android.gms.common.internal.zzbq.checkNotNull(looper, "Looper must not be null");
        com.google.android.gms.common.internal.zzbq.checkNotNull(str, "Listener type must not be null");
        return new zzci<>(looper, l, str);
    }

    public static <L> zzck<L> zzb(@NonNull L l, @NonNull String str) {
        com.google.android.gms.common.internal.zzbq.checkNotNull(l, "Listener must not be null");
        com.google.android.gms.common.internal.zzbq.checkNotNull(str, "Listener type must not be null");
        com.google.android.gms.common.internal.zzbq.zzh(str, "Listener type must not be empty");
        return new zzck<>(l, str);
    }

    public final void release() {
        Iterator<zzci<?>> it = this.zzfgd.iterator();
        while (it.hasNext()) {
            it.next().clear();
        }
        this.zzfgd.clear();
    }

    public final <L> zzci<L> zza(@NonNull L l, @NonNull Looper looper, @NonNull String str) {
        zzci<L> zzciVarZzb = zzb(l, looper, str);
        this.zzfgd.add(zzciVarZzb);
        return zzciVarZzb;
    }
}
