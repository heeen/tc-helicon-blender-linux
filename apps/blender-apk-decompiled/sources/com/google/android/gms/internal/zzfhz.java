package com.google.android.gms.internal;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/* JADX INFO: loaded from: classes.dex */
public final class zzfhz {
    public static final byte[] EMPTY_BYTE_ARRAY;
    private static ByteBuffer zzpqm;
    private static zzfhb zzpqn;
    static final Charset UTF_8 = Charset.forName("UTF-8");
    private static Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    static {
        byte[] bArr = new byte[0];
        EMPTY_BYTE_ARRAY = bArr;
        zzpqm = ByteBuffer.wrap(bArr);
        zzpqn = zzfhb.zzbb(EMPTY_BYTE_ARRAY);
    }

    static <T> T checkNotNull(T t) {
        if (t != null) {
            return t;
        }
        throw new NullPointerException();
    }

    public static int hashCode(byte[] bArr) {
        int length = bArr.length;
        int iZza = zza(length, bArr, 0, length);
        if (iZza == 0) {
            return 1;
        }
        return iZza;
    }

    static int zza(int i, byte[] bArr, int i2, int i3) {
        int i4 = i;
        for (int i5 = i2; i5 < i2 + i3; i5++) {
            i4 = (i4 * 31) + bArr[i5];
        }
        return i4;
    }

    static <T> T zzc(T t, String str) {
        if (t != null) {
            return t;
        }
        throw new NullPointerException(str);
    }

    public static int zzdf(long j) {
        return (int) (j ^ (j >>> 32));
    }

    public static int zzdo(boolean z) {
        return z ? 1231 : 1237;
    }

    static boolean zzh(zzfjc zzfjcVar) {
        return false;
    }
}
