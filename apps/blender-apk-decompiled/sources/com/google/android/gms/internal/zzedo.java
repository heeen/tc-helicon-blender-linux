package com.google.android.gms.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class zzedo<K, V> extends zzedq<K, V> {
    private final K[] zzmav;
    private final V[] zzmyg;
    private final Comparator<K> zzmyh;

    public zzedo(Comparator<K> comparator) {
        this.zzmav = (K[]) new Object[0];
        this.zzmyg = (V[]) new Object[0];
        this.zzmyh = comparator;
    }

    private zzedo(Comparator<K> comparator, K[] kArr, V[] vArr) {
        this.zzmav = kArr;
        this.zzmyg = vArr;
        this.zzmyh = comparator;
    }

    public static <A, B, C> zzedo<A, C> zza(List<A> list, Map<B, C> map, zzedt<A, B> zzedtVar, Comparator<A> comparator) {
        Collections.sort(list, comparator);
        int size = list.size();
        Object[] objArr = new Object[size];
        Object[] objArr2 = new Object[size];
        int i = 0;
        for (A a : list) {
            objArr[i] = a;
            objArr2[i] = map.get(zzedtVar.zzbo(a));
            i++;
        }
        return new zzedo<>(comparator, objArr, objArr2);
    }

    private static <T> T[] zza(T[] tArr, int i, T t) {
        T[] tArr2 = (T[]) new Object[tArr.length + 1];
        System.arraycopy(tArr, 0, tArr2, 0, i);
        tArr2[i] = t;
        System.arraycopy(tArr, i, tArr2, i + 1, (r0 - i) - 1);
        return tArr2;
    }

    private static <T> T[] zzb(T[] tArr, int i, T t) {
        int length = tArr.length;
        T[] tArr2 = (T[]) new Object[length];
        System.arraycopy(tArr, 0, tArr2, 0, length);
        tArr2[i] = t;
        return tArr2;
    }

    private final int zzbm(K k) {
        int i = 0;
        while (i < this.zzmav.length && this.zzmyh.compare(this.zzmav[i], k) < 0) {
            i++;
        }
        return i;
    }

    private final int zzbn(K k) {
        int i = 0;
        for (K k2 : this.zzmav) {
            if (this.zzmyh.compare(k, k2) == 0) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static <T> T[] zzc(T[] tArr, int i) {
        int length = tArr.length - 1;
        T[] tArr2 = (T[]) new Object[length];
        System.arraycopy(tArr, 0, tArr2, 0, i);
        System.arraycopy(tArr, i + 1, tArr2, i, length - i);
        return tArr2;
    }

    private final Iterator<Map.Entry<K, V>> zzj(int i, boolean z) {
        return new zzedp(this, i, z);
    }

    @Override // com.google.android.gms.internal.zzedq
    public final boolean containsKey(K k) {
        return zzbn(k) != -1;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final V get(K k) {
        int iZzbn = zzbn(k);
        if (iZzbn != -1) {
            return this.zzmyg[iZzbn];
        }
        return null;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final Comparator<K> getComparator() {
        return this.zzmyh;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final int indexOf(K k) {
        return zzbn(k);
    }

    @Override // com.google.android.gms.internal.zzedq
    public final boolean isEmpty() {
        return this.zzmav.length == 0;
    }

    @Override // com.google.android.gms.internal.zzedq, java.lang.Iterable
    public final Iterator<Map.Entry<K, V>> iterator() {
        return zzj(0, false);
    }

    @Override // com.google.android.gms.internal.zzedq
    public final int size() {
        return this.zzmav.length;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final void zza(zzeeb<K, V> zzeebVar) {
        for (int i = 0; i < this.zzmav.length; i++) {
            zzeebVar.zzh(this.zzmav[i], this.zzmyg[i]);
        }
    }

    @Override // com.google.android.gms.internal.zzedq
    public final zzedq<K, V> zzbj(K k) {
        int iZzbn = zzbn(k);
        if (iZzbn == -1) {
            return this;
        }
        return new zzedo(this.zzmyh, zzc(this.zzmav, iZzbn), zzc(this.zzmyg, iZzbn));
    }

    @Override // com.google.android.gms.internal.zzedq
    public final Iterator<Map.Entry<K, V>> zzbk(K k) {
        return zzj(zzbm(k), false);
    }

    @Override // com.google.android.gms.internal.zzedq
    public final K zzbl(K k) {
        int iZzbn = zzbn(k);
        if (iZzbn == -1) {
            throw new IllegalArgumentException("Can't find predecessor of nonexistent key");
        }
        if (iZzbn > 0) {
            return this.zzmav[iZzbn - 1];
        }
        return null;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final K zzbvp() {
        if (this.zzmav.length > 0) {
            return this.zzmav[0];
        }
        return null;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final K zzbvq() {
        if (this.zzmav.length > 0) {
            return this.zzmav[this.zzmav.length - 1];
        }
        return null;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final Iterator<Map.Entry<K, V>> zzbvr() {
        return zzj(this.zzmav.length - 1, true);
    }

    @Override // com.google.android.gms.internal.zzedq
    public final zzedq<K, V> zzg(K k, V v) {
        int iZzbn = zzbn(k);
        if (iZzbn != -1) {
            if (this.zzmav[iZzbn] == k && this.zzmyg[iZzbn] == v) {
                return this;
            }
            return new zzedo(this.zzmyh, zzb(this.zzmav, iZzbn, k), zzb(this.zzmyg, iZzbn, v));
        }
        if (this.zzmav.length <= 25) {
            int iZzbm = zzbm(k);
            return new zzedo(this.zzmyh, zza(this.zzmav, iZzbm, k), zza(this.zzmyg, iZzbm, v));
        }
        HashMap map = new HashMap(this.zzmav.length + 1);
        for (int i = 0; i < this.zzmav.length; i++) {
            map.put(this.zzmav[i], this.zzmyg[i]);
        }
        map.put(k, v);
        return zzeee.zzb(map, this.zzmyh);
    }
}
