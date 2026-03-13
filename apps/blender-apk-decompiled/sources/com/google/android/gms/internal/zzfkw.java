package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzfkw extends zzfkt {
    zzfkw() {
    }

    private static int zza(byte[] bArr, int i, long j, int i2) {
        switch (i2) {
            case 0:
                return zzfks.zzmu(i);
            case 1:
                return zzfks.zzam(i, zzfkq.zzb(bArr, j));
            case 2:
                return zzfks.zzi(i, zzfkq.zzb(bArr, j), zzfkq.zzb(bArr, j + 1));
            default:
                throw new AssertionError();
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:29:0x004f, code lost:
    
        return -1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x007b, code lost:
    
        return -1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static int zzb(byte[] r11, long r12, int r14) {
        /*
            r0 = 0
            r1 = 1
            r3 = 16
            if (r14 >= r3) goto L9
            r3 = r0
            goto L1b
        L9:
            r4 = r12
            r3 = r0
        Lb:
            if (r3 >= r14) goto L1a
            long r6 = r4 + r1
            byte r4 = com.google.android.gms.internal.zzfkq.zzb(r11, r4)
            if (r4 >= 0) goto L16
            goto L1b
        L16:
            int r3 = r3 + 1
            r4 = r6
            goto Lb
        L1a:
            r3 = r14
        L1b:
            int r14 = r14 - r3
            long r3 = (long) r3
            long r12 = r12 + r3
        L1e:
            r3 = r0
        L1f:
            if (r14 <= 0) goto L2f
            long r3 = r12 + r1
            byte r12 = com.google.android.gms.internal.zzfkq.zzb(r11, r12)
            if (r12 < 0) goto L32
            int r14 = r14 + (-1)
            r9 = r3
            r3 = r12
            r12 = r9
            goto L1f
        L2f:
            r9 = r12
            r12 = r3
            r3 = r9
        L32:
            if (r14 != 0) goto L35
            return r0
        L35:
            int r14 = r14 + (-1)
            r13 = -32
            r5 = -65
            r6 = -1
            if (r12 >= r13) goto L50
            if (r14 != 0) goto L41
            return r12
        L41:
            int r14 = r14 + (-1)
            r13 = -62
            if (r12 < r13) goto L4f
            long r12 = r3 + r1
            byte r3 = com.google.android.gms.internal.zzfkq.zzb(r11, r3)
            if (r3 <= r5) goto L1e
        L4f:
            return r6
        L50:
            r7 = -16
            if (r12 >= r7) goto L7c
            r7 = 2
            if (r14 >= r7) goto L5c
            int r11 = zza(r11, r12, r3, r14)
            return r11
        L5c:
            int r14 = r14 + (-2)
            long r7 = r3 + r1
            byte r3 = com.google.android.gms.internal.zzfkq.zzb(r11, r3)
            if (r3 > r5) goto L7b
            r4 = -96
            if (r12 != r13) goto L6c
            if (r3 < r4) goto L7b
        L6c:
            r13 = -19
            if (r12 != r13) goto L72
            if (r3 >= r4) goto L7b
        L72:
            r12 = 0
            long r12 = r7 + r1
            byte r3 = com.google.android.gms.internal.zzfkq.zzb(r11, r7)
            if (r3 <= r5) goto L1e
        L7b:
            return r6
        L7c:
            r13 = 3
            if (r14 >= r13) goto L84
            int r11 = zza(r11, r12, r3, r14)
            return r11
        L84:
            int r14 = r14 + (-3)
            long r7 = r3 + r1
            byte r13 = com.google.android.gms.internal.zzfkq.zzb(r11, r3)
            if (r13 > r5) goto Lab
            int r12 = r12 << 28
            int r13 = r13 + 112
            int r12 = r12 + r13
            int r12 = r12 >> 30
            if (r12 != 0) goto Lab
            long r12 = r7 + r1
            byte r3 = com.google.android.gms.internal.zzfkq.zzb(r11, r7)
            if (r3 > r5) goto Lab
            long r3 = r12 + r1
            byte r12 = com.google.android.gms.internal.zzfkq.zzb(r11, r12)
            if (r12 <= r5) goto La8
            goto Lab
        La8:
            r12 = r3
            goto L1e
        Lab:
            return r6
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.internal.zzfkw.zzb(byte[], long, int):int");
    }

    @Override // com.google.android.gms.internal.zzfkt
    final int zzb(int i, byte[] bArr, int i2, int i3) {
        if ((i2 | i3 | (bArr.length - i3)) < 0) {
            throw new ArrayIndexOutOfBoundsException(String.format("Array length=%d, index=%d, limit=%d", Integer.valueOf(bArr.length), Integer.valueOf(i2), Integer.valueOf(i3)));
        }
        long j = i2;
        return zzb(bArr, j, (int) (((long) i3) - j));
    }

    @Override // com.google.android.gms.internal.zzfkt
    final int zzb(CharSequence charSequence, byte[] bArr, int i, int i2) {
        long j;
        int i3;
        int i4;
        char cCharAt;
        long j2 = i;
        long j3 = ((long) i2) + j2;
        int length = charSequence.length();
        if (length > i2 || bArr.length - i2 < i) {
            char cCharAt2 = charSequence.charAt(length - 1);
            int i5 = i + i2;
            StringBuilder sb = new StringBuilder(37);
            sb.append("Failed writing ");
            sb.append(cCharAt2);
            sb.append(" at index ");
            sb.append(i5);
            throw new ArrayIndexOutOfBoundsException(sb.toString());
        }
        int i6 = 0;
        while (i6 < length && (cCharAt = charSequence.charAt(i6)) < 128) {
            zzfkq.zza(bArr, j2, (byte) cCharAt);
            i6++;
            j2 = 1 + j2;
        }
        if (i6 == length) {
            return (int) j2;
        }
        while (i6 < length) {
            char cCharAt3 = charSequence.charAt(i6);
            if (cCharAt3 >= 128 || j2 >= j3) {
                if (cCharAt3 < 2048 && j2 <= j3 - 2) {
                    long j4 = j2 + 1;
                    zzfkq.zza(bArr, j2, (byte) ((cCharAt3 >>> 6) | 960));
                    j2 = j4 + 1;
                    zzfkq.zza(bArr, j4, (byte) ((cCharAt3 & '?') | 128));
                } else {
                    if ((cCharAt3 >= 55296 && 57343 >= cCharAt3) || j2 > j3 - 3) {
                        if (j2 > j3 - 4) {
                            if (55296 <= cCharAt3 && cCharAt3 <= 57343 && ((i3 = i6 + 1) == length || !Character.isSurrogatePair(cCharAt3, charSequence.charAt(i3)))) {
                                throw new zzfkv(i6, length);
                            }
                            StringBuilder sb2 = new StringBuilder(46);
                            sb2.append("Failed writing ");
                            sb2.append(cCharAt3);
                            sb2.append(" at index ");
                            sb2.append(j2);
                            throw new ArrayIndexOutOfBoundsException(sb2.toString());
                        }
                        int i7 = i6 + 1;
                        if (i7 != length) {
                            char cCharAt4 = charSequence.charAt(i7);
                            if (Character.isSurrogatePair(cCharAt3, cCharAt4)) {
                                int codePoint = Character.toCodePoint(cCharAt3, cCharAt4);
                                long j5 = j2 + 1;
                                zzfkq.zza(bArr, j2, (byte) ((codePoint >>> 18) | 240));
                                long j6 = j5 + 1;
                                zzfkq.zza(bArr, j5, (byte) (((codePoint >>> 12) & 63) | 128));
                                long j7 = j6 + 1;
                                zzfkq.zza(bArr, j6, (byte) (((codePoint >>> 6) & 63) | 128));
                                j2 = j7 + 1;
                                zzfkq.zza(bArr, j7, (byte) ((codePoint & 63) | 128));
                                i6 = i7;
                            } else {
                                i6 = i7;
                            }
                        }
                        throw new zzfkv(i6 - 1, length);
                    }
                    long j8 = j2 + 1;
                    zzfkq.zza(bArr, j2, (byte) ((cCharAt3 >>> '\f') | 480));
                    j2 = j8 + 1;
                    zzfkq.zza(bArr, j8, (byte) (((cCharAt3 >>> 6) & 63) | 128));
                    j = j2 + 1;
                    i4 = (cCharAt3 & '?') | 128;
                }
                i6++;
            } else {
                j = j2 + 1;
                i4 = cCharAt3;
            }
            zzfkq.zza(bArr, j2, (byte) i4);
            j2 = j;
            i6++;
        }
        return (int) j2;
    }
}
