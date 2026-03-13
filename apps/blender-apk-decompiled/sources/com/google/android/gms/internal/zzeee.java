package com.google.android.gms.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class zzeee<K, V> extends zzedq<K, V> {
    private Comparator<K> zzmyh;
    private zzedz<K, V> zzmyx;

    private zzeee(zzedz<K, V> zzedzVar, Comparator<K> comparator) {
        this.zzmyx = zzedzVar;
        this.zzmyh = comparator;
    }

    public static <A, B> zzeee<A, B> zzb(Map<A, B> map, Comparator<A> comparator) {
        return zzeeg.zzc(new ArrayList(map.keySet()), map, zzedr.zzbvs(), comparator);
    }

    private final zzedz<K, V> zzbs(K k) {
        zzedz<K, V> zzedzVarZzbvy = this.zzmyx;
        while (!zzedzVarZzbvy.isEmpty()) {
            int iCompare = this.zzmyh.compare(k, zzedzVarZzbvy.getKey());
            if (iCompare < 0) {
                zzedzVarZzbvy = zzedzVarZzbvy.zzbvy();
            } else {
                if (iCompare == 0) {
                    return zzedzVarZzbvy;
                }
                zzedzVarZzbvy = zzedzVarZzbvy.zzbvz();
            }
        }
        return null;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final boolean containsKey(K k) {
        return zzbs(k) != null;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final V get(K k) {
        zzedz<K, V> zzedzVarZzbs = zzbs(k);
        if (zzedzVarZzbs != null) {
            return zzedzVarZzbs.getValue();
        }
        return null;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final Comparator<K> getComparator() {
        return this.zzmyh;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final int indexOf(K k) {
        zzedz<K, V> zzedzVarZzbvy = this.zzmyx;
        int size = 0;
        while (!zzedzVarZzbvy.isEmpty()) {
            int iCompare = this.zzmyh.compare(k, zzedzVarZzbvy.getKey());
            if (iCompare == 0) {
                return size + zzedzVarZzbvy.zzbvy().size();
            }
            if (iCompare < 0) {
                zzedzVarZzbvy = zzedzVarZzbvy.zzbvy();
            } else {
                size += zzedzVarZzbvy.zzbvy().size() + 1;
                zzedzVarZzbvy = zzedzVarZzbvy.zzbvz();
            }
        }
        return -1;
    }

    @Override // com.google.android.gms.internal.zzedq
    public final boolean isEmpty() {
        return this.zzmyx.isEmpty();
    }

    @Override // com.google.android.gms.internal.zzedq, java.lang.Iterable
    public final Iterator<Map.Entry<K, V>> iterator() {
        return new zzedu(this.zzmyx, null, this.zzmyh, false);
    }

    @Override // com.google.android.gms.internal.zzedq
    public final int size() {
        return this.zzmyx.size();
    }

    @Override // com.google.android.gms.internal.zzedq
    public final void zza(zzeeb<K, V> zzeebVar) {
        this.zzmyx.zza(zzeebVar);
    }

    @Override // com.google.android.gms.internal.zzedq
    public final zzedq<K, V> zzbj(K k) {
        return !containsKey(k) ? this : new zzeee(this.zzmyx.zza(k, this.zzmyh).zza(null, null, zzeea.zzmyt, null, null), this.zzmyh);
    }

    @Override // com.google.android.gms.internal.zzedq
    public final Iterator<Map.Entry<K, V>> zzbk(K k) {
        return new zzedu(this.zzmyx, k, this.zzmyh, false);
    }

    @Override // com.google.android.gms.internal.zzedq
    public final K zzbl(K k) {
        zzedz<K, V> zzedzVarZzbvy = this.zzmyx;
        zzedz<K, V> zzedzVar = null;
        while (!zzedzVarZzbvy.isEmpty()) {
            int iCompare = this.zzmyh.compare(k, zzedzVarZzbvy.getKey());
            if (iCompare == 0) {
                if (zzedzVarZzbvy.zzbvy().isEmpty()) {
                    if (zzedzVar != null) {
                        return zzedzVar.getKey();
                    }
                    return null;
                }
                zzedz<K, V> zzedzVarZzbvy2 = zzedzVarZzbvy.zzbvy();
                while (!zzedzVarZzbvy2.zzbvz().isEmpty()) {
                    zzedzVarZzbvy2 = zzedzVarZzbvy2.zzbvz();
                }
                return zzedzVarZzbvy2.getKey();
            }
            if (iCompare < 0) {
                zzedzVarZzbvy = zzedzVarZzbvy.zzbvy();
            } else {
                zzedzVar = zzedzVarZzbvy;
                zzedzVarZzbvy = zzedzVarZzbvy.zzbvz();
            }
        }
        String strValueOf = String.valueOf(k);
        StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 50);
        sb.append("Couldn't find predecessor key of non-present key: ");
        sb.append(strValueOf);
        throw new IllegalArgumentException(sb.toString());
    }

    @Override // com.google.android.gms.internal.zzedq
    public final K zzbvp() {
        return this.zzmyx.zzbwa().getKey();
    }

    @Override // com.google.android.gms.internal.zzedq
    public final K zzbvq() {
        return this.zzmyx.zzbwb().getKey();
    }

    @Override // com.google.android.gms.internal.zzedq
    public final Iterator<Map.Entry<K, V>> zzbvr() {
        return new zzedu(this.zzmyx, null, this.zzmyh, true);
    }

    @Override // com.google.android.gms.internal.zzedq
    public final zzedq<K, V> zzg(K k, V v) {
        return new zzeee(this.zzmyx.zza(k, v, this.zzmyh).zza(null, null, zzeea.zzmyt, null, null), this.zzmyh);
    }
}
