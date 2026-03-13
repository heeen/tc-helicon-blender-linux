package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
public final class zzeec<K, V> extends zzeed<K, V> {
    zzeec(K k, V v) {
        super(k, v, zzedy.zzbvx(), zzedy.zzbvx());
    }

    zzeec(K k, V v, zzedz<K, V> zzedzVar, zzedz<K, V> zzedzVar2) {
        super(k, v, zzedzVar, zzedzVar2);
    }

    @Override // com.google.android.gms.internal.zzedz
    public final int size() {
        return zzbvy().size() + 1 + zzbvz().size();
    }

    @Override // com.google.android.gms.internal.zzeed
    protected final zzeed<K, V> zza(K k, V v, zzedz<K, V> zzedzVar, zzedz<K, V> zzedzVar2) {
        if (k == null) {
            k = getKey();
        }
        if (v == null) {
            v = getValue();
        }
        if (zzedzVar == null) {
            zzedzVar = zzbvy();
        }
        if (zzedzVar2 == null) {
            zzedzVar2 = zzbvz();
        }
        return new zzeec(k, v, zzedzVar, zzedzVar2);
    }

    @Override // com.google.android.gms.internal.zzeed
    protected final int zzbvv() {
        return zzeea.zzmys;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final boolean zzbvw() {
        return true;
    }
}
