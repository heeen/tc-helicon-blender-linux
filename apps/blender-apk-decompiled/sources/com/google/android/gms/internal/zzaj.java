package com.google.android.gms.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/* JADX INFO: loaded from: classes.dex */
public class zzaj implements zzm {
    private static boolean DEBUG = zzaf.DEBUG;

    @Deprecated
    private zzar zzbo;
    private final zzai zzbp;
    private zzak zzbq;

    public zzaj(zzai zzaiVar) {
        this(zzaiVar, new zzak(4096));
    }

    private zzaj(zzai zzaiVar, zzak zzakVar) {
        this.zzbp = zzaiVar;
        this.zzbo = zzaiVar;
        this.zzbq = zzakVar;
    }

    @Deprecated
    public zzaj(zzar zzarVar) {
        this(zzarVar, new zzak(4096));
    }

    @Deprecated
    private zzaj(zzar zzarVar, zzak zzakVar) {
        this.zzbo = zzarVar;
        this.zzbp = new zzah(zzarVar);
        this.zzbq = zzakVar;
    }

    private static List<zzl> zza(List<zzl> list, zzc zzcVar) {
        TreeSet treeSet = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        if (!list.isEmpty()) {
            Iterator<zzl> it = list.iterator();
            while (it.hasNext()) {
                treeSet.add(it.next().getName());
            }
        }
        ArrayList arrayList = new ArrayList(list);
        if (zzcVar.zzg != null) {
            if (!zzcVar.zzg.isEmpty()) {
                for (zzl zzlVar : zzcVar.zzg) {
                    if (!treeSet.contains(zzlVar.getName())) {
                        arrayList.add(zzlVar);
                    }
                }
            }
        } else if (!zzcVar.zzf.isEmpty()) {
            for (Map.Entry<String, String> entry : zzcVar.zzf.entrySet()) {
                if (!treeSet.contains(entry.getKey())) {
                    arrayList.add(new zzl(entry.getKey(), entry.getValue()));
                }
            }
        }
        return arrayList;
    }

    private static void zza(String str, zzr<?> zzrVar, zzae zzaeVar) throws zzae {
        zzab zzabVarZzi = zzrVar.zzi();
        int iZzh = zzrVar.zzh();
        try {
            zzabVarZzi.zza(zzaeVar);
            zzrVar.zzb(String.format("%s-retry [timeout=%s]", str, Integer.valueOf(iZzh)));
        } catch (zzae e) {
            zzrVar.zzb(String.format("%s-timeout-giveup [timeout=%s]", str, Integer.valueOf(iZzh)));
            throw e;
        }
    }

    private final byte[] zza(InputStream inputStream, int i) throws Throwable {
        zzau zzauVar = new zzau(this.zzbq, i);
        byte[] bArr = null;
        try {
            if (inputStream == null) {
                throw new zzac();
            }
            byte[] bArrZzb = this.zzbq.zzb(1024);
            while (true) {
                try {
                    int i2 = inputStream.read(bArrZzb);
                    if (i2 == -1) {
                        break;
                    }
                    zzauVar.write(bArrZzb, 0, i2);
                } catch (Throwable th) {
                    th = th;
                    bArr = bArrZzb;
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException unused) {
                            zzaf.zza("Error occurred when closing InputStream", new Object[0]);
                        }
                    }
                    this.zzbq.zza(bArr);
                    zzauVar.close();
                    throw th;
                }
            }
            byte[] byteArray = zzauVar.toByteArray();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException unused2) {
                    zzaf.zza("Error occurred when closing InputStream", new Object[0]);
                }
            }
            this.zzbq.zza(bArrZzb);
            zzauVar.close();
            return byteArray;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:54:0x0106, code lost:
    
        throw new java.io.IOException();
     */
    /* JADX WARN: Removed duplicated region for block: B:119:0x0188 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:68:0x0121  */
    @Override // com.google.android.gms.internal.zzm
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public com.google.android.gms.internal.zzp zzc(com.google.android.gms.internal.zzr<?> r27) throws java.lang.Throwable {
        /*
            Method dump skipped, instruction units count: 443
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.internal.zzaj.zzc(com.google.android.gms.internal.zzr):com.google.android.gms.internal.zzp");
    }
}
