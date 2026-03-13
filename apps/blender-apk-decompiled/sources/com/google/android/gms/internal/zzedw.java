package com.google.android.gms.internal;

import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class zzedw<T> implements Iterator<T> {
    private Iterator<Map.Entry<T, Void>> zzmyq;

    public zzedw(Iterator<Map.Entry<T, Void>> it) {
        this.zzmyq = it;
    }

    @Override // java.util.Iterator
    public final boolean hasNext() {
        return this.zzmyq.hasNext();
    }

    @Override // java.util.Iterator
    public final T next() {
        return this.zzmyq.next().getKey();
    }

    @Override // java.util.Iterator
    public final void remove() {
        this.zzmyq.remove();
    }
}
