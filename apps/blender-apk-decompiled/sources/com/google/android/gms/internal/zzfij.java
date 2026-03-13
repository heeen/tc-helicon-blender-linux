package com.google.android.gms.internal;

import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class zzfij<K> implements Iterator<Map.Entry<K, Object>> {
    private Iterator<Map.Entry<K, Object>> zzmyq;

    public zzfij(Iterator<Map.Entry<K, Object>> it) {
        this.zzmyq = it;
    }

    @Override // java.util.Iterator
    public final boolean hasNext() {
        return this.zzmyq.hasNext();
    }

    @Override // java.util.Iterator
    public final /* synthetic */ Object next() {
        Map.Entry<K, Object> next = this.zzmyq.next();
        return next.getValue() instanceof zzfig ? new zzfii(next) : next;
    }

    @Override // java.util.Iterator
    public final void remove() {
        this.zzmyq.remove();
    }
}
