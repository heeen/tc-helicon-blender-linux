package com.google.android.gms.common.util;

/* JADX INFO: loaded from: classes.dex */
public final class zzn {
    /* JADX WARN: Removed duplicated region for block: B:24:0x0067  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static java.lang.String zza(byte[] r8, int r9, int r10, boolean r11) {
        /*
            if (r8 == 0) goto L75
            int r9 = r8.length
            if (r9 == 0) goto L75
            if (r10 <= 0) goto L75
            int r9 = r8.length
            if (r10 <= r9) goto Lc
            goto L75
        Lc:
            int r9 = r10 + 16
            r11 = 1
            int r9 = r9 - r11
            r0 = 16
            int r9 = r9 / r0
            int r9 = r9 * 57
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>(r9)
            r9 = 0
            r3 = r9
            r4 = r3
            r2 = r10
        L1e:
            if (r2 <= 0) goto L70
            if (r3 != 0) goto L43
            r5 = 65536(0x10000, float:9.1835E-41)
            if (r10 >= r5) goto L38
            java.lang.String r5 = "%04X:"
            java.lang.Object[] r6 = new java.lang.Object[r11]
            java.lang.Integer r7 = java.lang.Integer.valueOf(r4)
            r6[r9] = r7
        L30:
            java.lang.String r5 = java.lang.String.format(r5, r6)
        L34:
            r1.append(r5)
            goto L4a
        L38:
            java.lang.String r5 = "%08X:"
            java.lang.Object[] r6 = new java.lang.Object[r11]
            java.lang.Integer r7 = java.lang.Integer.valueOf(r4)
            r6[r9] = r7
            goto L30
        L43:
            r5 = 8
            if (r3 != r5) goto L4a
            java.lang.String r5 = " -"
            goto L34
        L4a:
            java.lang.String r5 = " %02X"
            java.lang.Object[] r6 = new java.lang.Object[r11]
            r7 = r8[r4]
            r7 = r7 & 255(0xff, float:3.57E-43)
            java.lang.Integer r7 = java.lang.Integer.valueOf(r7)
            r6[r9] = r7
            java.lang.String r5 = java.lang.String.format(r5, r6)
            r1.append(r5)
            int r2 = r2 + (-1)
            int r3 = r3 + 1
            if (r3 == r0) goto L67
            if (r2 != 0) goto L6d
        L67:
            r3 = 10
            r1.append(r3)
            r3 = r9
        L6d:
            int r4 = r4 + 1
            goto L1e
        L70:
            java.lang.String r8 = r1.toString()
            return r8
        L75:
            r8 = 0
            return r8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.common.util.zzn.zza(byte[], int, int, boolean):java.lang.String");
    }
}
