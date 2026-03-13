package com.google.android.gms.internal;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;

/* JADX INFO: Add missing generic type declarations: [V, K] */
/* JADX INFO: loaded from: classes.dex */
final class zzedp<K, V> implements Iterator<Map.Entry<K, V>> {
    private int zzmyi;
    private /* synthetic */ int zzmyj;
    private /* synthetic */ boolean zzmyk;
    private /* synthetic */ zzedo zzmyl;

    zzedp(zzedo zzedoVar, int i, boolean z) {
        this.zzmyl = zzedoVar;
        this.zzmyj = i;
        this.zzmyk = z;
        this.zzmyi = this.zzmyj;
    }

    @Override // java.util.Iterator
    public final boolean hasNext() {
        return this.zzmyk ? this.zzmyi >= 0 : this.zzmyi < this.zzmyl.zzmav.length;
    }

    @Override // java.util.Iterator
    public final /* synthetic */ Object next() {
        Object obj = this.zzmyl.zzmav[this.zzmyi];
        Object obj2 = this.zzmyl.zzmyg[this.zzmyi];
        this.zzmyi = this.zzmyk ? this.zzmyi - 1 : this.zzmyi + 1;
        return new AbstractMap.SimpleImmutableEntry(obj, obj2);
    }

    @Override // java.util.Iterator
    public final void remove() {
        throw new UnsupportedOperationException("Can't remove elements from ImmutableSortedMap");
    }
}
