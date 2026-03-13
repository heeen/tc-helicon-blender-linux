package com.google.android.gms.internal;

import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class zzfjf {
    static String zza(zzfjc zzfjcVar, String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ");
        sb.append(str);
        zza(zzfjcVar, sb, 0);
        return sb.toString();
    }

    /* JADX WARN: Removed duplicated region for block: B:64:0x019a  */
    /* JADX WARN: Removed duplicated region for block: B:65:0x019d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static void zza(com.google.android.gms.internal.zzfjc r12, java.lang.StringBuilder r13, int r14) {
        /*
            Method dump skipped, instruction units count: 586
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.internal.zzfjf.zza(com.google.android.gms.internal.zzfjc, java.lang.StringBuilder, int):void");
    }

    static final void zzb(StringBuilder sb, int i, String str, Object obj) {
        if (obj instanceof List) {
            Iterator it = ((List) obj).iterator();
            while (it.hasNext()) {
                zzb(sb, i, str, it.next());
            }
            return;
        }
        sb.append('\n');
        for (int i2 = 0; i2 < i; i2++) {
            sb.append(' ');
        }
        sb.append(str);
        if (obj instanceof String) {
            sb.append(": \"");
            sb.append(zzfkh.zzbd(zzfgs.zztv((String) obj)));
            sb.append('\"');
            return;
        }
        if (obj instanceof zzfgs) {
            sb.append(": \"");
            sb.append(zzfkh.zzbd((zzfgs) obj));
            sb.append('\"');
        } else {
            if (!(obj instanceof zzfhu)) {
                sb.append(": ");
                sb.append(obj.toString());
                return;
            }
            sb.append(" {");
            zza((zzfhu) obj, sb, i + 2);
            sb.append("\n");
            for (int i3 = 0; i3 < i; i3++) {
                sb.append(' ');
            }
            sb.append("}");
        }
    }

    private static final String zztz(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (Character.isUpperCase(cCharAt)) {
                sb.append("_");
            }
            sb.append(Character.toLowerCase(cCharAt));
        }
        return sb.toString();
    }
}
