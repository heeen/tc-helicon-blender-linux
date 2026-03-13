package com.google.android.gms.internal;

import com.google.android.gms.internal.zzfhu;

/* JADX INFO: loaded from: classes.dex */
final class zzfiq implements zzfjw {
    private static final zzfjb zzpqx = new zzfir();
    private final zzfjb zzpqw;

    public zzfiq() {
        this(new zzfis(zzfht.zzczp(), zzdas()));
    }

    private zzfiq(zzfjb zzfjbVar) {
        this.zzpqw = (zzfjb) zzfhz.zzc(zzfjbVar, "messageInfoFactory");
    }

    private static boolean zza(zzfja zzfjaVar) {
        return zzfjaVar.zzdaz() == zzfhu.zzg.zzpqc;
    }

    private static zzfjb zzdas() {
        try {
            return (zzfjb) Class.forName("com.google.protobuf.DescriptorMessageInfoFactory").getDeclaredMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
        } catch (Exception unused) {
            return zzpqx;
        }
    }

    @Override // com.google.android.gms.internal.zzfjw
    public final <T> zzfjv<T> zzk(Class<T> cls) {
        zzfjx.zzm(cls);
        zzfja zzfjaVarZzj = this.zzpqw.zzj(cls);
        return zzfjaVarZzj.zzdba() ? zzfhu.class.isAssignableFrom(cls) ? zzfjh.zza(cls, zzfjx.zzdbm(), zzfhp.zzczh(), zzfjaVarZzj.zzdbb()) : zzfjh.zza(cls, zzfjx.zzdbk(), zzfhp.zzczi(), zzfjaVarZzj.zzdbb()) : zzfhu.class.isAssignableFrom(cls) ? zza(zzfjaVarZzj) ? zzfjg.zza(cls, zzfjaVarZzj, zzfjk.zzdbd(), zzfim.zzdar(), zzfjx.zzdbm(), zzfhp.zzczh(), zzfiz.zzdax()) : zzfjg.zza(cls, zzfjaVarZzj, zzfjk.zzdbd(), zzfim.zzdar(), zzfjx.zzdbm(), null, zzfiz.zzdax()) : zza(zzfjaVarZzj) ? zzfjg.zza(cls, zzfjaVarZzj, zzfjk.zzdbc(), zzfim.zzdaq(), zzfjx.zzdbk(), zzfhp.zzczi(), zzfiz.zzdaw()) : zzfjg.zza(cls, zzfjaVarZzj, zzfjk.zzdbc(), zzfim.zzdaq(), zzfjx.zzdbl(), null, zzfiz.zzdaw());
    }
}
