package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzfks {
    private static final zzfkt zzptx;

    static {
        zzptx = zzfkq.zzdcd() && zzfkq.zzdce() ? new zzfkw() : new zzfku();
    }

    static int zza(CharSequence charSequence, byte[] bArr, int i, int i2) {
        return zzptx.zzb(charSequence, bArr, i, i2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int zzam(int i, int i2) {
        if (i > -12 || i2 > -65) {
            return -1;
        }
        return i ^ (i2 << 8);
    }

    static int zzd(CharSequence charSequence) {
        int length = charSequence.length();
        int i = 0;
        int i2 = 0;
        while (i2 < length && charSequence.charAt(i2) < 128) {
            i2++;
        }
        int i3 = length;
        while (true) {
            if (i2 >= length) {
                break;
            }
            char cCharAt = charSequence.charAt(i2);
            if (cCharAt < 2048) {
                i3 += (127 - cCharAt) >>> 31;
                i2++;
            } else {
                int length2 = charSequence.length();
                while (i2 < length2) {
                    char cCharAt2 = charSequence.charAt(i2);
                    if (cCharAt2 < 2048) {
                        i += (127 - cCharAt2) >>> 31;
                    } else {
                        i += 2;
                        if (55296 <= cCharAt2 && cCharAt2 <= 57343) {
                            if (Character.codePointAt(charSequence, i2) < 65536) {
                                throw new zzfkv(i2, length2);
                            }
                            i2++;
                        }
                    }
                    i2++;
                }
                i3 += i;
            }
        }
        if (i3 >= length) {
            return i3;
        }
        long j = ((long) i3) + 4294967296L;
        StringBuilder sb = new StringBuilder(54);
        sb.append("UTF-8 length does not fit in int: ");
        sb.append(j);
        throw new IllegalArgumentException(sb.toString());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int zzi(int i, int i2, int i3) {
        if (i > -12 || i2 > -65 || i3 > -65) {
            return -1;
        }
        return (i ^ (i2 << 8)) ^ (i3 << 16);
    }

    public static boolean zzl(byte[] bArr, int i, int i2) {
        return zzptx.zzb(0, bArr, i, i2) == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int zzm(byte[] bArr, int i, int i2) {
        byte b = bArr[i - 1];
        switch (i2 - i) {
            case 0:
                return zzmu(b);
            case 1:
                return zzam(b, bArr[i]);
            case 2:
                return zzi(b, bArr[i], bArr[i + 1]);
            default:
                throw new AssertionError();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int zzmu(int i) {
        if (i > -12) {
            return -1;
        }
        return i;
    }
}
