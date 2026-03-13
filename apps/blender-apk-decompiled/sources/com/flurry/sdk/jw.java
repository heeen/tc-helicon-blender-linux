package com.flurry.sdk;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class jw<K, V> {
    public final Map<K, List<V>> a;
    private int b;

    public jw() {
        this.a = new HashMap();
    }

    public jw(Map<K, List<V>> map) {
        this.a = map;
    }

    public final void a() {
        this.a.clear();
    }

    public final List<V> a(K k) {
        if (k == null) {
            return Collections.emptyList();
        }
        List<V> listA = a((Object) k, false);
        return listA == null ? Collections.emptyList() : listA;
    }

    public final void a(K k, V v) {
        if (k == null) {
            return;
        }
        a((Object) k, true).add(v);
    }

    public final void a(jw<K, V> jwVar) {
        if (jwVar == null) {
            return;
        }
        for (Map.Entry<K, List<V>> entry : jwVar.a.entrySet()) {
            a((Object) entry.getKey(), true).addAll(entry.getValue());
        }
    }

    public final boolean b(K k, V v) {
        List<V> listA;
        if (k == null || (listA = a((Object) k, false)) == null) {
            return false;
        }
        boolean zRemove = listA.remove(v);
        if (listA.size() == 0) {
            this.a.remove(k);
        }
        return zRemove;
    }

    public final boolean b(K k) {
        return (k == null || this.a.remove(k) == null) ? false : true;
    }

    public final Collection<Map.Entry<K, V>> b() {
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<K, List<V>> entry : this.a.entrySet()) {
            Iterator<V> it = entry.getValue().iterator();
            while (it.hasNext()) {
                arrayList.add(new AbstractMap.SimpleImmutableEntry(entry.getKey(), it.next()));
            }
        }
        return arrayList;
    }

    public final Collection<V> c() {
        ArrayList arrayList = new ArrayList();
        Iterator<Map.Entry<K, List<V>>> it = this.a.entrySet().iterator();
        while (it.hasNext()) {
            arrayList.addAll(it.next().getValue());
        }
        return arrayList;
    }

    public final List<V> a(K k, boolean z) {
        ArrayList arrayList;
        List<V> list = this.a.get(k);
        if (z && list == null) {
            if (this.b > 0) {
                arrayList = new ArrayList(this.b);
            } else {
                arrayList = new ArrayList();
            }
            list = arrayList;
            this.a.put(k, list);
        }
        return list;
    }
}
