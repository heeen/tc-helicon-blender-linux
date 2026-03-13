package com.google.android.gms.internal;

import java.util.Iterator;
import java.util.Map;

/* JADX INFO: Add missing generic type declarations: [V, K] */
/* JADX INFO: loaded from: classes.dex */
final class zzfke<K, V> implements Iterator<Map.Entry<K, V>> {
    private int pos;
    private /* synthetic */ zzfjy zzpss;
    private boolean zzpst;
    private Iterator<Map.Entry<K, V>> zzpsu;

    private zzfke(zzfjy zzfjyVar) {
        this.zzpss = zzfjyVar;
        this.pos = -1;
    }

    /* synthetic */ zzfke(zzfjy zzfjyVar, zzfjz zzfjzVar) {
        this(zzfjyVar);
    }

    private final Iterator<Map.Entry<K, V>> zzdbv() {
        if (this.zzpsu == null) {
            this.zzpsu = this.zzpss.zzpsm.entrySet().iterator();
        }
        return this.zzpsu;
    }

    @Override // java.util.Iterator
    public final boolean hasNext() {
        return this.pos + 1 < this.zzpss.zzpsl.size() || (!this.zzpss.zzpsm.isEmpty() && zzdbv().hasNext());
    }

    @Override // java.util.Iterator
    public final /* synthetic */ Object next() {
        this.zzpst = true;
        int i = this.pos + 1;
        this.pos = i;
        return i < this.zzpss.zzpsl.size() ? (Map.Entry<K, V>) this.zzpss.zzpsl.get(this.pos) : zzdbv().next();
    }

    @Override // java.util.Iterator
    public final void remove() {
        if (!this.zzpst) {
            throw new IllegalStateException("remove() was called before next()");
        }
        this.zzpst = false;
        this.zzpss.zzdbr();
        if (this.pos >= this.zzpss.zzpsl.size()) {
            zzdbv().remove();
            return;
        }
        zzfjy zzfjyVar = this.zzpss;
        int i = this.pos;
        this.pos = i - 1;
        zzfjyVar.zzms(i);
    }
}
