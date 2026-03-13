package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
public final class zzedx<K, V> extends zzeed<K, V> {
    private int size;

    zzedx(K k, V v, zzedz<K, V> zzedzVar, zzedz<K, V> zzedzVar2) {
        super(k, v, zzedzVar, zzedzVar2);
        this.size = -1;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final int size() {
        if (this.size == -1) {
            this.size = zzbvy().size() + 1 + zzbvz().size();
        }
        return this.size;
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
        return new zzedx(k, v, zzedzVar, zzedzVar2);
    }

    @Override // com.google.android.gms.internal.zzeed
    final void zza(zzedz<K, V> zzedzVar) {
        if (this.size != -1) {
            throw new IllegalStateException("Can't set left after using size");
        }
        super.zza(zzedzVar);
    }

    @Override // com.google.android.gms.internal.zzeed
    protected final int zzbvv() {
        return zzeea.zzmyt;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final boolean zzbvw() {
        return false;
    }
}
