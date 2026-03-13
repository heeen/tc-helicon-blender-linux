package com.google.android.gms.internal;

import java.util.Comparator;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzeed<K, V> implements zzedz<K, V> {
    private final V value;
    private final K zzmbd;
    private zzedz<K, V> zzmyv;
    private final zzedz<K, V> zzmyw;

    zzeed(K k, V v, zzedz<K, V> zzedzVar, zzedz<K, V> zzedzVar2) {
        this.zzmbd = k;
        this.value = v;
        this.zzmyv = zzedzVar == null ? zzedy.zzbvx() : zzedzVar;
        this.zzmyw = zzedzVar2 == null ? zzedy.zzbvx() : zzedzVar2;
    }

    private static int zzb(zzedz zzedzVar) {
        return zzedzVar.zzbvw() ? zzeea.zzmyt : zzeea.zzmys;
    }

    /* JADX WARN: Incorrect types in method signature: (TK;TV;Ljava/lang/Integer;Lcom/google/android/gms/internal/zzedz<TK;TV;>;Lcom/google/android/gms/internal/zzedz<TK;TV;>;)Lcom/google/android/gms/internal/zzeed<TK;TV;>; */
    private final zzeed zzb(Object obj, Object obj2, int i, zzedz zzedzVar, zzedz zzedzVar2) {
        K k = this.zzmbd;
        V v = this.value;
        if (zzedzVar == null) {
            zzedzVar = this.zzmyv;
        }
        if (zzedzVar2 == null) {
            zzedzVar2 = this.zzmyw;
        }
        return i == zzeea.zzmys ? new zzeec(k, v, zzedzVar, zzedzVar2) : new zzedx(k, v, zzedzVar, zzedzVar2);
    }

    private final zzedz<K, V> zzbwc() {
        if (this.zzmyv.isEmpty()) {
            return zzedy.zzbvx();
        }
        if (!this.zzmyv.zzbvw() && !this.zzmyv.zzbvy().zzbvw()) {
            this = zzbwd();
        }
        return this.zza(null, null, ((zzeed) this.zzmyv).zzbwc(), null).zzbwe();
    }

    private final zzeed<K, V> zzbwd() {
        zzeed<K, V> zzeedVarZzbwh = zzbwh();
        return zzeedVarZzbwh.zzmyw.zzbvy().zzbvw() ? zzeedVarZzbwh.zza(null, null, null, ((zzeed) zzeedVarZzbwh.zzmyw).zzbwg()).zzbwf().zzbwh() : zzeedVarZzbwh;
    }

    private final zzeed<K, V> zzbwe() {
        if (this.zzmyw.zzbvw() && !this.zzmyv.zzbvw()) {
            this = zzbwf();
        }
        if (this.zzmyv.zzbvw() && ((zzeed) this.zzmyv).zzmyv.zzbvw()) {
            this = this.zzbwg();
        }
        return (this.zzmyv.zzbvw() && this.zzmyw.zzbvw()) ? this.zzbwh() : this;
    }

    private final zzeed<K, V> zzbwf() {
        return (zzeed) this.zzmyw.zza(null, null, zzbvv(), zzb(null, null, zzeea.zzmys, null, ((zzeed) this.zzmyw).zzmyv), null);
    }

    private final zzeed<K, V> zzbwg() {
        return (zzeed) this.zzmyv.zza(null, null, zzbvv(), null, zzb(null, null, zzeea.zzmys, ((zzeed) this.zzmyv).zzmyw, null));
    }

    private final zzeed<K, V> zzbwh() {
        return zzb(null, null, zzb(this), this.zzmyv.zza(null, null, zzb(this.zzmyv), null, null), this.zzmyw.zza(null, null, zzb(this.zzmyw), null, null));
    }

    @Override // com.google.android.gms.internal.zzedz
    public final K getKey() {
        return this.zzmbd;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final V getValue() {
        return this.value;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final boolean isEmpty() {
        return false;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final /* synthetic */ zzedz zza(Object obj, Object obj2, int i, zzedz zzedzVar, zzedz zzedzVar2) {
        return zzb(null, null, i, zzedzVar, zzedzVar2);
    }

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zza(K k, V v, Comparator<K> comparator) {
        int iCompare = comparator.compare(k, this.zzmbd);
        return (iCompare < 0 ? zza(null, null, this.zzmyv.zza(k, v, comparator), null) : iCompare == 0 ? zza(k, v, null, null) : zza(null, null, null, this.zzmyw.zza(k, v, comparator))).zzbwe();
    }

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zza(K k, Comparator<K> comparator) {
        zzeed<K, V> zzeedVarZza;
        if (comparator.compare(k, this.zzmbd) < 0) {
            if (!this.zzmyv.isEmpty() && !this.zzmyv.zzbvw() && !((zzeed) this.zzmyv).zzmyv.zzbvw()) {
                this = zzbwd();
            }
            zzeedVarZza = this.zza(null, null, this.zzmyv.zza(k, comparator), null);
        } else {
            if (this.zzmyv.zzbvw()) {
                this = zzbwg();
            }
            if (!this.zzmyw.isEmpty() && !this.zzmyw.zzbvw() && !((zzeed) this.zzmyw).zzmyv.zzbvw()) {
                this = this.zzbwh();
                if (this.zzmyv.zzbvy().zzbvw()) {
                    this = this.zzbwg().zzbwh();
                }
            }
            if (comparator.compare(k, this.zzmbd) == 0) {
                if (this.zzmyw.isEmpty()) {
                    return zzedy.zzbvx();
                }
                zzedz<K, V> zzedzVarZzbwa = this.zzmyw.zzbwa();
                this = this.zza(zzedzVarZzbwa.getKey(), zzedzVarZzbwa.getValue(), null, ((zzeed) this.zzmyw).zzbwc());
            }
            zzeedVarZza = this.zza(null, null, null, this.zzmyw.zza(k, comparator));
        }
        return zzeedVarZza.zzbwe();
    }

    protected abstract zzeed<K, V> zza(K k, V v, zzedz<K, V> zzedzVar, zzedz<K, V> zzedzVar2);

    void zza(zzedz<K, V> zzedzVar) {
        this.zzmyv = zzedzVar;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final void zza(zzeeb<K, V> zzeebVar) {
        this.zzmyv.zza(zzeebVar);
        zzeebVar.zzh(this.zzmbd, this.value);
        this.zzmyw.zza(zzeebVar);
    }

    protected abstract int zzbvv();

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zzbvy() {
        return this.zzmyv;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zzbvz() {
        return this.zzmyw;
    }

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zzbwa() {
        return this.zzmyv.isEmpty() ? this : this.zzmyv.zzbwa();
    }

    @Override // com.google.android.gms.internal.zzedz
    public final zzedz<K, V> zzbwb() {
        return this.zzmyw.isEmpty() ? this : this.zzmyw.zzbwb();
    }
}
