package com.google.android.gms.internal;

import java.util.Comparator;

/* JADX INFO: loaded from: classes.dex */
public interface zzedz<K, V> {
    K getKey();

    V getValue();

    boolean isEmpty();

    int size();

    /* JADX WARN: Incorrect types in method signature: (TK;TV;Ljava/lang/Integer;Lcom/google/android/gms/internal/zzedz<TK;TV;>;Lcom/google/android/gms/internal/zzedz<TK;TV;>;)Lcom/google/android/gms/internal/zzedz<TK;TV;>; */
    zzedz zza(Object obj, Object obj2, int i, zzedz zzedzVar, zzedz zzedzVar2);

    zzedz<K, V> zza(K k, V v, Comparator<K> comparator);

    zzedz<K, V> zza(K k, Comparator<K> comparator);

    void zza(zzeeb<K, V> zzeebVar);

    boolean zzbvw();

    zzedz<K, V> zzbvy();

    zzedz<K, V> zzbvz();

    zzedz<K, V> zzbwa();

    zzedz<K, V> zzbwb();
}
