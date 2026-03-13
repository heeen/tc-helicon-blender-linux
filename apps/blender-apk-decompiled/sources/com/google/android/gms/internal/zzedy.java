package com.google.android.gms.internal;

import java.util.Comparator;

/* JADX INFO: loaded from: classes.dex */
public final class zzedy<K, V> implements zzedz<K, V> {
    private static final zzedy zzmyr = new zzedy();

    private zzedy() {
    }

    public static <K, V> zzedy<K, V> zzbvx() {
        return zzmyr;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final K getKey() {
        return null;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final V getValue() {
        return null;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final boolean isEmpty() {
        return true;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final int size() {
        return 0;
    }

    /* JADX WARN: Incorrect types in method signature: (TK;TV;Ljava/lang/Integer;Lcom/google/android/gms/internal/zzedz<TK;TV;>;Lcom/google/android/gms/internal/zzedz<TK;TV;>;)Lcom/google/android/gms/internal/zzedz<TK;TV;>; */
    @Override // com.google.android.gms.internal.zzedz
    public final zzedz zza(Object obj, Object obj2, int i, zzedz zzedzVar, zzedz zzedzVar2) {
        return this;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zza(K k, V v, Comparator<K> comparator) {
        return new zzeec(k, v);
    }

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zza(K k, Comparator<K> comparator) {
        return this;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final void zza(zzeeb<K, V> zzeebVar) {
    }

    @Override // com.google.android.gms.internal.zzedz
    public final boolean zzbvw() {
        return false;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zzbvy() {
        return this;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zzbvz() {
        return this;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zzbwa() {
        return this;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zzbwb() {
        return this;
    }
}
