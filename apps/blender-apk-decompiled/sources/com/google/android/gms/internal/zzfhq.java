package com.google.android.gms.internal;

import com.google.android.gms.internal.zzfhs;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class zzfhq<FieldDescriptorType extends zzfhs<FieldDescriptorType>> {
    private static final zzfhq zzppc = new zzfhq(true);
    private boolean zzldh;
    private boolean zzppb = false;
    private final zzfjy<FieldDescriptorType, Object> zzppa = zzfjy.zzmq(16);

    private zzfhq() {
    }

    private zzfhq(boolean z) {
        if (this.zzldh) {
            return;
        }
        this.zzppa.zzbkr();
        this.zzldh = true;
    }

    static int zza(zzfky zzfkyVar, int i, Object obj) {
        int iZzlw = zzfhg.zzlw(i);
        if (zzfkyVar == zzfky.zzpuh) {
            zzfhz.zzh((zzfjc) obj);
            iZzlw <<= 1;
        }
        return iZzlw + zzb(zzfkyVar, obj);
    }

    public static Object zza(zzfhb zzfhbVar, zzfky zzfkyVar, boolean z) throws IOException {
        zzfle zzfleVar = zzfle.zzpvf;
        switch (zzfkx.zzppe[zzfkyVar.ordinal()]) {
            case 1:
                return Double.valueOf(zzfhbVar.readDouble());
            case 2:
                return Float.valueOf(zzfhbVar.readFloat());
            case 3:
                return Long.valueOf(zzfhbVar.zzcxz());
            case 4:
                return Long.valueOf(zzfhbVar.zzcxy());
            case 5:
                return Integer.valueOf(zzfhbVar.zzcya());
            case 6:
                return Long.valueOf(zzfhbVar.zzcyb());
            case 7:
                return Integer.valueOf(zzfhbVar.zzcyc());
            case 8:
                return Boolean.valueOf(zzfhbVar.zzcyd());
            case 9:
                return zzfhbVar.zzcyf();
            case 10:
                return Integer.valueOf(zzfhbVar.zzcyg());
            case 11:
                return Integer.valueOf(zzfhbVar.zzcyi());
            case 12:
                return Long.valueOf(zzfhbVar.zzcyj());
            case 13:
                return Integer.valueOf(zzfhbVar.zzcyk());
            case 14:
                return Long.valueOf(zzfhbVar.zzcyl());
            case 15:
                return zzfleVar.zza(zzfhbVar);
            case 16:
                throw new IllegalArgumentException("readPrimitiveField() cannot handle nested groups.");
            case 17:
                throw new IllegalArgumentException("readPrimitiveField() cannot handle embedded messages.");
            case 18:
                throw new IllegalArgumentException("readPrimitiveField() cannot handle enums.");
            default:
                throw new RuntimeException("There is no way to get here, but the compiler thinks otherwise.");
        }
    }

    static void zza(zzfhg zzfhgVar, zzfky zzfkyVar, int i, Object obj) throws IOException {
        if (zzfkyVar == zzfky.zzpuh) {
            zzfjc zzfjcVar = (zzfjc) obj;
            zzfhz.zzh(zzfjcVar);
            zzfhgVar.zze(i, zzfjcVar);
        }
        zzfhgVar.zzac(i, zzfkyVar.zzdcj());
        switch (zzfhr.zzppe[zzfkyVar.ordinal()]) {
            case 1:
                zzfhgVar.zzn(((Double) obj).doubleValue());
                break;
            case 2:
                zzfhgVar.zzf(((Float) obj).floatValue());
                break;
            case 3:
                zzfhgVar.zzcu(((Long) obj).longValue());
                break;
            case 4:
                zzfhgVar.zzcu(((Long) obj).longValue());
                break;
            case 5:
                zzfhgVar.zzls(((Integer) obj).intValue());
                break;
            case 6:
                zzfhgVar.zzcw(((Long) obj).longValue());
                break;
            case 7:
                zzfhgVar.zzlv(((Integer) obj).intValue());
                break;
            case 8:
                zzfhgVar.zzdl(((Boolean) obj).booleanValue());
                break;
            case 9:
                ((zzfjc) obj).zza(zzfhgVar);
                break;
            case 10:
                zzfhgVar.zze((zzfjc) obj);
                break;
            case 11:
                if (!(obj instanceof zzfgs)) {
                    zzfhgVar.zztw((String) obj);
                } else {
                    zzfhgVar.zzay((zzfgs) obj);
                }
                break;
            case 12:
                if (!(obj instanceof zzfgs)) {
                    byte[] bArr = (byte[]) obj;
                    zzfhgVar.zzj(bArr, 0, bArr.length);
                } else {
                    zzfhgVar.zzay((zzfgs) obj);
                }
                break;
            case 13:
                zzfhgVar.zzlt(((Integer) obj).intValue());
                break;
            case 14:
                zzfhgVar.zzlv(((Integer) obj).intValue());
                break;
            case 15:
                zzfhgVar.zzcw(((Long) obj).longValue());
                break;
            case 16:
                zzfhgVar.zzlu(((Integer) obj).intValue());
                break;
            case 17:
                zzfhgVar.zzcv(((Long) obj).longValue());
                break;
            case 18:
                if (!(obj instanceof zzfia)) {
                    zzfhgVar.zzls(((Integer) obj).intValue());
                } else {
                    zzfhgVar.zzls(((zzfia) obj).zzhu());
                }
                break;
        }
    }

    private void zza(FieldDescriptorType fielddescriptortype, Object obj) {
        if (!fielddescriptortype.zzczn()) {
            zza(fielddescriptortype.zzczl(), obj);
        } else {
            if (!(obj instanceof List)) {
                throw new IllegalArgumentException("Wrong object type used with protocol message reflection.");
            }
            ArrayList arrayList = new ArrayList();
            arrayList.addAll((List) obj);
            ArrayList arrayList2 = arrayList;
            int size = arrayList2.size();
            int i = 0;
            while (i < size) {
                Object obj2 = arrayList2.get(i);
                i++;
                zza(fielddescriptortype.zzczl(), obj2);
            }
            obj = arrayList;
        }
        if (obj instanceof zzfig) {
            this.zzppb = true;
        }
        this.zzppa.put(fielddescriptortype, obj);
    }

    /* JADX WARN: Removed duplicated region for block: B:14:0x0026 A[PHI: r0
      0x0026: PHI (r0v8 boolean) = 
      (r0v2 boolean)
      (r0v3 boolean)
      (r0v4 boolean)
      (r0v5 boolean)
      (r0v6 boolean)
      (r0v7 boolean)
      (r0v1 boolean)
      (r0v1 boolean)
      (r0v1 boolean)
      (r0v1 boolean)
      (r0v1 boolean)
      (r0v1 boolean)
     binds: [B:25:0x0040, B:24:0x003d, B:23:0x003a, B:22:0x0037, B:21:0x0034, B:20:0x0031, B:16:0x002a, B:18:0x002e, B:11:0x0020, B:13:0x0024, B:6:0x0017, B:8:0x001b] A[DONT_GENERATE, DONT_INLINE]] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static void zza(com.google.android.gms.internal.zzfky r2, java.lang.Object r3) {
        /*
            com.google.android.gms.internal.zzfhz.checkNotNull(r3)
            int[] r0 = com.google.android.gms.internal.zzfhr.zzppd
            com.google.android.gms.internal.zzfld r2 = r2.zzdci()
            int r2 = r2.ordinal()
            r2 = r0[r2]
            r0 = 1
            r1 = 0
            switch(r2) {
                case 1: goto L40;
                case 2: goto L3d;
                case 3: goto L3a;
                case 4: goto L37;
                case 5: goto L34;
                case 6: goto L31;
                case 7: goto L28;
                case 8: goto L1e;
                case 9: goto L15;
                default: goto L14;
            }
        L14:
            goto L43
        L15:
            boolean r2 = r3 instanceof com.google.android.gms.internal.zzfjc
            if (r2 != 0) goto L26
            boolean r2 = r3 instanceof com.google.android.gms.internal.zzfig
            if (r2 == 0) goto L43
            goto L26
        L1e:
            boolean r2 = r3 instanceof java.lang.Integer
            if (r2 != 0) goto L26
            boolean r2 = r3 instanceof com.google.android.gms.internal.zzfia
            if (r2 == 0) goto L43
        L26:
            r1 = r0
            goto L43
        L28:
            boolean r2 = r3 instanceof com.google.android.gms.internal.zzfgs
            if (r2 != 0) goto L26
            boolean r2 = r3 instanceof byte[]
            if (r2 == 0) goto L43
            goto L26
        L31:
            boolean r0 = r3 instanceof java.lang.String
            goto L26
        L34:
            boolean r0 = r3 instanceof java.lang.Boolean
            goto L26
        L37:
            boolean r0 = r3 instanceof java.lang.Double
            goto L26
        L3a:
            boolean r0 = r3 instanceof java.lang.Float
            goto L26
        L3d:
            boolean r0 = r3 instanceof java.lang.Long
            goto L26
        L40:
            boolean r0 = r3 instanceof java.lang.Integer
            goto L26
        L43:
            if (r1 == 0) goto L46
            return
        L46:
            java.lang.IllegalArgumentException r2 = new java.lang.IllegalArgumentException
            java.lang.String r3 = "Wrong object type used with protocol message reflection."
            r2.<init>(r3)
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.internal.zzfhq.zza(com.google.android.gms.internal.zzfky, java.lang.Object):void");
    }

    private static int zzb(zzfhs<?> zzfhsVar, Object obj) {
        zzfky zzfkyVarZzczl = zzfhsVar.zzczl();
        int iZzhu = zzfhsVar.zzhu();
        if (!zzfhsVar.zzczn()) {
            return zza(zzfkyVarZzczl, iZzhu, obj);
        }
        int iZza = 0;
        if (zzfhsVar.zzczo()) {
            Iterator it = ((List) obj).iterator();
            while (it.hasNext()) {
                iZza += zzb(zzfkyVarZzczl, it.next());
            }
            return zzfhg.zzlw(iZzhu) + iZza + zzfhg.zzmf(iZza);
        }
        Iterator it2 = ((List) obj).iterator();
        while (it2.hasNext()) {
            iZza += zza(zzfkyVarZzczl, iZzhu, it2.next());
        }
        return iZza;
    }

    private static int zzb(zzfky zzfkyVar, Object obj) {
        switch (zzfhr.zzppe[zzfkyVar.ordinal()]) {
            case 1:
                return zzfhg.zzo(((Double) obj).doubleValue());
            case 2:
                return zzfhg.zzg(((Float) obj).floatValue());
            case 3:
                return zzfhg.zzcx(((Long) obj).longValue());
            case 4:
                return zzfhg.zzcy(((Long) obj).longValue());
            case 5:
                return zzfhg.zzlx(((Integer) obj).intValue());
            case 6:
                return zzfhg.zzda(((Long) obj).longValue());
            case 7:
                return zzfhg.zzma(((Integer) obj).intValue());
            case 8:
                return zzfhg.zzdm(((Boolean) obj).booleanValue());
            case 9:
                return zzfhg.zzg((zzfjc) obj);
            case 10:
                return obj instanceof zzfig ? zzfhg.zza((zzfig) obj) : zzfhg.zzf((zzfjc) obj);
            case 11:
                return obj instanceof zzfgs ? zzfhg.zzaz((zzfgs) obj) : zzfhg.zztx((String) obj);
            case 12:
                return obj instanceof zzfgs ? zzfhg.zzaz((zzfgs) obj) : zzfhg.zzbd((byte[]) obj);
            case 13:
                return zzfhg.zzly(((Integer) obj).intValue());
            case 14:
                return zzfhg.zzmb(((Integer) obj).intValue());
            case 15:
                return zzfhg.zzdb(((Long) obj).longValue());
            case 16:
                return zzfhg.zzlz(((Integer) obj).intValue());
            case 17:
                return zzfhg.zzcz(((Long) obj).longValue());
            case 18:
                return obj instanceof zzfia ? zzfhg.zzmc(((zzfia) obj).zzhu()) : zzfhg.zzmc(((Integer) obj).intValue());
            default:
                throw new RuntimeException("There is no way to get here, but the compiler thinks otherwise.");
        }
    }

    private static int zzb(Map.Entry<FieldDescriptorType, Object> entry) {
        FieldDescriptorType key = entry.getKey();
        Object value = entry.getValue();
        return (key.zzczm() != zzfld.MESSAGE || key.zzczn() || key.zzczo()) ? zzb((zzfhs<?>) key, value) : value instanceof zzfig ? zzfhg.zzb(entry.getKey().zzhu(), (zzfig) value) : zzfhg.zzd(entry.getKey().zzhu(), (zzfjc) value);
    }

    public static <T extends zzfhs<T>> zzfhq<T> zzczj() {
        return zzppc;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public final /* synthetic */ Object clone() throws CloneNotSupportedException {
        zzfhq zzfhqVar = new zzfhq();
        for (int i = 0; i < this.zzppa.zzdbp(); i++) {
            Map.Entry<K, Object> entryZzmr = this.zzppa.zzmr(i);
            zzfhqVar.zza((zzfhs) entryZzmr.getKey(), entryZzmr.getValue());
        }
        Iterator it = this.zzppa.zzdbq().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            zzfhqVar.zza((zzfhs) entry.getKey(), entry.getValue());
        }
        zzfhqVar.zzppb = this.zzppb;
        return zzfhqVar;
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof zzfhq) {
            return this.zzppa.equals(((zzfhq) obj).zzppa);
        }
        return false;
    }

    public final int hashCode() {
        return this.zzppa.hashCode();
    }

    public final Iterator<Map.Entry<FieldDescriptorType, Object>> iterator() {
        return this.zzppb ? new zzfij(this.zzppa.entrySet().iterator()) : this.zzppa.entrySet().iterator();
    }

    public final int zzczk() {
        int iZzb = 0;
        for (int i = 0; i < this.zzppa.zzdbp(); i++) {
            iZzb += zzb(this.zzppa.zzmr(i));
        }
        Iterator it = this.zzppa.zzdbq().iterator();
        while (it.hasNext()) {
            iZzb += zzb((Map.Entry) it.next());
        }
        return iZzb;
    }
}
