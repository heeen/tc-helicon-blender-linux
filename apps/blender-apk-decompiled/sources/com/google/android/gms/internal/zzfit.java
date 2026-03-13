package com.google.android.gms.internal;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class zzfit<K, V> {
    private final V value;
    private final K zzmbd;
    private final zzfiv<K, V> zzpqz;

    private zzfit(zzfky zzfkyVar, K k, zzfky zzfkyVar2, V v) {
        this.zzpqz = new zzfiv<>(zzfkyVar, k, zzfkyVar2, v);
        this.zzmbd = k;
        this.value = v;
    }

    static <K, V> int zza(zzfiv<K, V> zzfivVar, K k, V v) {
        return zzfhq.zza(zzfivVar.zzpra, 1, k) + zzfhq.zza(zzfivVar.zzprc, 2, v);
    }

    public static <K, V> zzfit<K, V> zza(zzfky zzfkyVar, K k, zzfky zzfkyVar2, V v) {
        return new zzfit<>(zzfkyVar, k, zzfkyVar2, v);
    }

    private static <T> T zza(zzfhb zzfhbVar, zzfhm zzfhmVar, zzfky zzfkyVar, T t) throws IOException {
        switch (zzfiu.zzppe[zzfkyVar.ordinal()]) {
            case 1:
                zzfjd zzfjdVarZzczt = ((zzfjc) t).zzczt();
                zzfhbVar.zza(zzfjdVarZzczt, zzfhmVar);
                return (T) zzfjdVarZzczt.zzczy();
            case 2:
                return (T) Integer.valueOf(zzfhbVar.zzcyh());
            case 3:
                throw new RuntimeException("Groups are not allowed in maps.");
            default:
                return (T) zzfhq.zza(zzfhbVar, zzfkyVar, true);
        }
    }

    static <K, V> void zza(zzfhg zzfhgVar, zzfiv<K, V> zzfivVar, K k, V v) throws IOException {
        zzfhq.zza(zzfhgVar, zzfivVar.zzpra, 1, k);
        zzfhq.zza(zzfhgVar, zzfivVar.zzprc, 2, v);
    }

    public final void zza(zzfhg zzfhgVar, int i, K k, V v) throws IOException {
        zzfhgVar.zzac(i, 2);
        zzfhgVar.zzlt(zza(this.zzpqz, k, v));
        zza(zzfhgVar, this.zzpqz, k, v);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final void zza(zzfiw<K, V> zzfiwVar, zzfhb zzfhbVar, zzfhm zzfhmVar) throws IOException {
        int iZzli = zzfhbVar.zzli(zzfhbVar.zzcym());
        Object objZza = this.zzpqz.zzprb;
        Object objZza2 = this.zzpqz.zzinq;
        while (true) {
            int iZzcxx = zzfhbVar.zzcxx();
            if (iZzcxx == 0) {
                break;
            }
            if (iZzcxx == (this.zzpqz.zzpra.zzdcj() | 8)) {
                objZza = zza(zzfhbVar, zzfhmVar, this.zzpqz.zzpra, objZza);
            } else if (iZzcxx == (this.zzpqz.zzprc.zzdcj() | 16)) {
                objZza2 = zza(zzfhbVar, zzfhmVar, this.zzpqz.zzprc, objZza2);
            } else if (!zzfhbVar.zzlg(iZzcxx)) {
                break;
            }
        }
        zzfhbVar.zzlf(0);
        zzfhbVar.zzlj(iZzli);
        zzfiwVar.put(objZza, objZza2);
    }

    public final int zzb(int i, K k, V v) {
        return zzfhg.zzlw(i) + zzfhg.zzmd(zza(this.zzpqz, k, v));
    }
}
