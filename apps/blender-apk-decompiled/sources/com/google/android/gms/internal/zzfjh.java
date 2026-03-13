package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzfjh<T> implements zzfjv<T> {
    private final zzfjc zzprg;
    private final zzfkn<?, ?> zzprh;
    private final boolean zzpri;
    private final zzfhn<?> zzprj;

    private zzfjh(Class<T> cls, zzfkn<?, ?> zzfknVar, zzfhn<?> zzfhnVar, zzfjc zzfjcVar) {
        this.zzprh = zzfknVar;
        this.zzpri = zzfhnVar.zzh(cls);
        this.zzprj = zzfhnVar;
        this.zzprg = zzfjcVar;
    }

    static <T> zzfjh<T> zza(Class<T> cls, zzfkn<?, ?> zzfknVar, zzfhn<?> zzfhnVar, zzfjc zzfjcVar) {
        return new zzfjh<>(cls, zzfknVar, zzfhnVar, zzfjcVar);
    }

    @Override // com.google.android.gms.internal.zzfjv
    public final void zza(T t, zzfli zzfliVar) {
        int iZzhu;
        Object value;
        for (T t2 : this.zzprj.zzcr(t)) {
            zzfhs zzfhsVar = (zzfhs) t2.getKey();
            if (zzfhsVar.zzczm() != zzfld.MESSAGE || zzfhsVar.zzczn() || zzfhsVar.zzczo()) {
                throw new IllegalStateException("Found invalid MessageSet item.");
            }
            if (t2 instanceof zzfii) {
                iZzhu = zzfhsVar.zzhu();
                value = ((zzfii) t2).zzdao().toByteString();
            } else {
                iZzhu = zzfhsVar.zzhu();
                value = t2.getValue();
            }
            zzfliVar.zzb(iZzhu, value);
        }
        zzfkn<?, ?> zzfknVar = this.zzprh;
        zzfknVar.zzb(zzfknVar.zzcu(t), zzfliVar);
    }

    @Override // com.google.android.gms.internal.zzfjv
    public final int zzct(T t) {
        zzfkn<?, ?> zzfknVar = this.zzprh;
        int iZzcv = zzfknVar.zzcv(zzfknVar.zzcu(t)) + 0;
        return this.zzpri ? iZzcv + this.zzprj.zzcr(t).zzczk() : iZzcv;
    }
}
