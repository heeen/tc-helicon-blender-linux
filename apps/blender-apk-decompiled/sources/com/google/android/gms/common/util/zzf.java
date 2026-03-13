package com.google.android.gms.common.util;

import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class zzf {
    private static <K, V> Map<K, V> zza(int i, boolean z, K[] kArr, V[] vArr) {
        Map<K, V> mapZzh = zzh(i, false);
        for (int i2 = 0; i2 < kArr.length; i2++) {
            mapZzh.put(kArr[i2], vArr[i2]);
        }
        return mapZzh;
    }

    public static <K, V> Map<K, V> zza(K k, V v, K k2, V v2, K k3, V v3) {
        Map mapZzh = zzh(3, false);
        mapZzh.put(k, v);
        mapZzh.put(k2, v2);
        mapZzh.put(k3, v3);
        return Collections.unmodifiableMap(mapZzh);
    }

    public static <K, V> Map<K, V> zza(K k, V v, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        Map mapZzh = zzh(6, false);
        mapZzh.put(k, v);
        mapZzh.put(k2, v2);
        mapZzh.put(k3, v3);
        mapZzh.put(k4, v4);
        mapZzh.put(k5, v5);
        mapZzh.put(k6, v6);
        return Collections.unmodifiableMap(mapZzh);
    }

    public static <T> Set<T> zza(T t, T t2, T t3) {
        Set setZzg = zzg(3, false);
        setZzg.add(t);
        setZzg.add(t2);
        setZzg.add(t3);
        return Collections.unmodifiableSet(setZzg);
    }

    public static <K, V> Map<K, V> zzb(K[] kArr, V[] vArr) {
        if (kArr.length == vArr.length) {
            switch (kArr.length) {
                case 0:
                    return Collections.emptyMap();
                case 1:
                    return Collections.singletonMap(kArr[0], vArr[0]);
                default:
                    return Collections.unmodifiableMap(zza(kArr.length, false, kArr, vArr));
            }
        }
        int length = kArr.length;
        int length2 = vArr.length;
        StringBuilder sb = new StringBuilder(66);
        sb.append("Key and values array lengths not equal: ");
        sb.append(length);
        sb.append(" != ");
        sb.append(length2);
        throw new IllegalArgumentException(sb.toString());
    }

    public static <T> Set<T> zzb(T... tArr) {
        switch (tArr.length) {
            case 0:
                return Collections.emptySet();
            case 1:
                return Collections.singleton(tArr[0]);
            case 2:
                T t = tArr[0];
                T t2 = tArr[1];
                Set setZzg = zzg(2, false);
                setZzg.add(t);
                setZzg.add(t2);
                return Collections.unmodifiableSet(setZzg);
            case 3:
                return zza(tArr[0], tArr[1], tArr[2]);
            case 4:
                T t3 = tArr[0];
                T t4 = tArr[1];
                T t5 = tArr[2];
                T t6 = tArr[3];
                Set setZzg2 = zzg(4, false);
                setZzg2.add(t3);
                setZzg2.add(t4);
                setZzg2.add(t5);
                setZzg2.add(t6);
                return Collections.unmodifiableSet(setZzg2);
            default:
                Set setZzg3 = zzg(tArr.length, false);
                Collections.addAll(setZzg3, tArr);
                return Collections.unmodifiableSet(setZzg3);
        }
    }

    private static <T> Set<T> zzg(int i, boolean z) {
        return i <= 256 ? new ArraySet(i) : new HashSet(i, 1.0f);
    }

    private static <K, V> Map<K, V> zzh(int i, boolean z) {
        return i <= 256 ? new ArrayMap(i) : new HashMap(i, 1.0f);
    }
}
