package com.google.android.gms.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class zzedr {
    private static final zzedt zzmym = new zzeds();

    public static <K, V> zzedq<K, V> zza(Comparator<K> comparator) {
        return new zzedo(comparator);
    }

    public static <A, B> zzedq<A, B> zza(Map<A, B> map, Comparator<A> comparator) {
        return map.size() < 25 ? zzedo.zza(new ArrayList(map.keySet()), map, zzmym, comparator) : zzeee.zzb(map, comparator);
    }

    public static <A, B, C> zzedq<A, C> zzb(List<A> list, Map<B, C> map, zzedt<A, B> zzedtVar, Comparator<A> comparator) {
        return list.size() < 25 ? zzedo.zza(list, map, zzedtVar, comparator) : zzeeg.zzc(list, map, zzedtVar, comparator);
    }

    public static <A> zzedt<A, A> zzbvs() {
        return zzmym;
    }
}
