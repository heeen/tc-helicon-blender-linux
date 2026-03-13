package com.google.android.gms.internal;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzedq<K, V> implements Iterable<Map.Entry<K, V>> {
    public abstract boolean containsKey(K k);

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof zzedq)) {
            return false;
        }
        zzedq zzedqVar = (zzedq) obj;
        if (!getComparator().equals(zzedqVar.getComparator()) || size() != zzedqVar.size()) {
            return false;
        }
        Iterator<Map.Entry<K, V>> it = iterator();
        Iterator<Map.Entry<K, V>> it2 = zzedqVar.iterator();
        while (it.hasNext()) {
            if (!it.next().equals(it2.next())) {
                return false;
            }
        }
        return true;
    }

    public abstract V get(K k);

    public abstract Comparator<K> getComparator();

    public int hashCode() {
        int iHashCode = getComparator().hashCode();
        Iterator<Map.Entry<K, V>> it = iterator();
        while (it.hasNext()) {
            iHashCode = (iHashCode * 31) + it.next().hashCode();
        }
        return iHashCode;
    }

    public abstract int indexOf(K k);

    public abstract boolean isEmpty();

    @Override // java.lang.Iterable
    public abstract Iterator<Map.Entry<K, V>> iterator();

    public abstract int size();

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("{");
        boolean z = true;
        for (Map.Entry<K, V> entry : this) {
            if (z) {
                z = false;
            } else {
                sb.append(", ");
            }
            sb.append("(");
            sb.append(entry.getKey());
            sb.append("=>");
            sb.append(entry.getValue());
            sb.append(")");
        }
        sb.append("};");
        return sb.toString();
    }

    public abstract void zza(zzeeb<K, V> zzeebVar);

    public abstract zzedq<K, V> zzbj(K k);

    public abstract Iterator<Map.Entry<K, V>> zzbk(K k);

    public abstract K zzbl(K k);

    public abstract K zzbvp();

    public abstract K zzbvq();

    public abstract Iterator<Map.Entry<K, V>> zzbvr();

    public abstract zzedq<K, V> zzg(K k, V v);
}
