package com.google.android.gms.internal;

import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class zzfii<K> implements Map.Entry<K, Object> {
    private Map.Entry<K, zzfig> zzpqp;

    private zzfii(Map.Entry<K, zzfig> entry) {
        this.zzpqp = entry;
    }

    @Override // java.util.Map.Entry
    public final K getKey() {
        return this.zzpqp.getKey();
    }

    @Override // java.util.Map.Entry
    public final Object getValue() {
        if (this.zzpqp.getValue() == null) {
            return null;
        }
        return zzfig.zzdan();
    }

    @Override // java.util.Map.Entry
    public final Object setValue(Object obj) {
        if (obj instanceof zzfjc) {
            return this.zzpqp.getValue().zzk((zzfjc) obj);
        }
        throw new IllegalArgumentException("LazyField now only used for MessageSet, and the value of MessageSet must be an instance of MessageLite");
    }

    public final zzfig zzdao() {
        return this.zzpqp.getValue();
    }
}
