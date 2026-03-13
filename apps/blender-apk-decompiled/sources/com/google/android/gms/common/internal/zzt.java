package com.google.android.gms.common.internal;

import com.google.android.gms.common.api.Scope;
import java.util.Collections;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class zzt {
    public final Set<Scope> zzenh;

    public zzt(Set<Scope> set) {
        zzbq.checkNotNull(set);
        this.zzenh = Collections.unmodifiableSet(set);
    }
}
