package com.google.android.gms.internal;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

/* JADX INFO: loaded from: classes.dex */
public final class zzedu<K, V> implements Iterator<Map.Entry<K, V>> {
    private final Stack<zzeed<K, V>> zzmyn = new Stack<>();
    private final boolean zzmyo;

    zzedu(zzedz<K, V> zzedzVar, K k, Comparator<K> comparator, boolean z) {
        this.zzmyo = z;
        while (!zzedzVar.isEmpty()) {
            int iCompare = k != null ? z ? comparator.compare(k, zzedzVar.getKey()) : comparator.compare(zzedzVar.getKey(), k) : 1;
            if (iCompare < 0) {
                zzedzVar = !z ? zzedzVar.zzbvz() : zzedzVar.zzbvy();
            } else if (iCompare == 0) {
                this.zzmyn.push((zzeed) zzedzVar);
                return;
            } else {
                this.zzmyn.push((zzeed) zzedzVar);
                if (z) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Override // java.util.Iterator
    public final Map.Entry<K, V> next() {
        try {
            zzeed<K, V> zzeedVarPop = this.zzmyn.pop();
            AbstractMap.SimpleEntry simpleEntry = new AbstractMap.SimpleEntry(zzeedVarPop.getKey(), zzeedVarPop.getValue());
            if (this.zzmyo) {
                for (zzedz<K, V> zzedzVarZzbvy = zzeedVarPop.zzbvy(); !zzedzVarZzbvy.isEmpty(); zzedzVarZzbvy = zzedzVarZzbvy.zzbvz()) {
                    this.zzmyn.push((zzeed) zzedzVarZzbvy);
                }
            } else {
                for (zzedz<K, V> zzedzVarZzbvz = zzeedVarPop.zzbvz(); !zzedzVarZzbvz.isEmpty(); zzedzVarZzbvz = zzedzVarZzbvz.zzbvy()) {
                    this.zzmyn.push((zzeed) zzedzVarZzbvz);
                }
            }
            return simpleEntry;
        } catch (EmptyStackException unused) {
            throw new NoSuchElementException();
        }
    }

    @Override // java.util.Iterator
    public final boolean hasNext() {
        return this.zzmyn.size() > 0;
    }

    @Override // java.util.Iterator
    public final void remove() {
        throw new UnsupportedOperationException("remove called on immutable collection");
    }
}
