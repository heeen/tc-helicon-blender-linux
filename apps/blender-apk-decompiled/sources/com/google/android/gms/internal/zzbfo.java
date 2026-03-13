package com.google.android.gms.internal;

import com.flurry.android.Constants;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* JADX INFO: loaded from: classes.dex */
public final class zzbfo {
    private static long zza(long j, long j2, long j3) {
        long j4 = (j ^ j2) * j3;
        long j5 = ((j4 ^ (j4 >>> 47)) ^ j2) * j3;
        return (j5 ^ (j5 >>> 47)) * j3;
    }

    private static long zza(byte[] bArr, int i, int i2) {
        long[] jArr = new long[2];
        long[] jArr2 = new long[2];
        long jZzc = zzc(bArr, 0) + 95310865018149119L;
        int i3 = i2 - 1;
        int i4 = ((i3 / 64) << 6) + 0;
        int i5 = i3 & 63;
        int i6 = (i4 + i5) - 63;
        long j = 2480279821605975764L;
        long j2 = 1390051526045402406L;
        int i7 = i;
        while (true) {
            long jRotateRight = Long.rotateRight(jZzc + j + jArr[0] + zzc(bArr, i7 + 8), 37) * (-5435081209227447693L);
            long jRotateRight2 = Long.rotateRight(j + jArr[1] + zzc(bArr, i7 + 48), 42) * (-5435081209227447693L);
            long j3 = jRotateRight ^ jArr2[1];
            long jZzc2 = jRotateRight2 + jArr[0] + zzc(bArr, i7 + 40);
            long jRotateRight3 = Long.rotateRight(j2 + jArr2[0], 33) * (-5435081209227447693L);
            zza(bArr, i7, jArr[1] * (-5435081209227447693L), j3 + jArr2[0], jArr);
            zza(bArr, i7 + 32, jRotateRight3 + jArr2[1], jZzc2 + zzc(bArr, i7 + 16), jArr2);
            i7 += 64;
            if (i7 == i4) {
                long j4 = ((j3 & 255) << 1) - 5435081209227447693L;
                jArr2[0] = jArr2[0] + ((long) i5);
                jArr[0] = jArr[0] + jArr2[0];
                jArr2[0] = jArr2[0] + jArr[0];
                long jRotateRight4 = Long.rotateRight(jRotateRight3 + jZzc2 + jArr[0] + zzc(bArr, i6 + 8), 37) * j4;
                long jRotateRight5 = Long.rotateRight(jZzc2 + jArr[1] + zzc(bArr, i6 + 48), 42) * j4;
                long j5 = jRotateRight4 ^ (jArr2[1] * 9);
                long jZzc3 = jRotateRight5 + (jArr[0] * 9) + zzc(bArr, i6 + 40);
                long jRotateRight6 = Long.rotateRight(j3 + jArr2[0], 33) * j4;
                zza(bArr, i6, jArr[1] * j4, j5 + jArr2[0], jArr);
                zza(bArr, i6 + 32, jRotateRight6 + jArr2[1], zzc(bArr, i6 + 16) + jZzc3, jArr2);
                return zza(zza(jArr[0], jArr2[0], j4) + (((jZzc3 >>> 47) ^ jZzc3) * (-4348849565147123417L)) + j5, zza(jArr[1], jArr2[1], j4) + jRotateRight6, j4);
            }
            j2 = j3;
            j = jZzc2;
            jZzc = jRotateRight3;
        }
    }

    private static void zza(byte[] bArr, int i, long j, long j2, long[] jArr) {
        long jZzc = zzc(bArr, i);
        long jZzc2 = zzc(bArr, i + 8);
        long jZzc3 = zzc(bArr, i + 16);
        long jZzc4 = zzc(bArr, i + 24);
        long j3 = j + jZzc;
        long j4 = jZzc2 + j3 + jZzc3;
        long jRotateRight = Long.rotateRight(j2 + j3 + jZzc4, 21) + Long.rotateRight(j4, 44);
        jArr[0] = j4 + jZzc4;
        jArr[1] = jRotateRight + j3;
    }

    private static int zzb(byte[] bArr, int i) {
        return ((bArr[i + 3] & Constants.UNKNOWN) << 24) | (bArr[i] & Constants.UNKNOWN) | ((bArr[i + 1] & Constants.UNKNOWN) << 8) | ((bArr[i + 2] & Constants.UNKNOWN) << 16);
    }

    private static long zzc(byte[] bArr, int i) {
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr, i, 8);
        byteBufferWrap.order(ByteOrder.LITTLE_ENDIAN);
        return byteBufferWrap.getLong();
    }

    public static long zzi(byte[] bArr) {
        int length = bArr.length;
        if (length < 0 || length > bArr.length) {
            StringBuilder sb = new StringBuilder(67);
            sb.append("Out of bound index with offput: 0 and length: ");
            sb.append(length);
            throw new IndexOutOfBoundsException(sb.toString());
        }
        if (length > 32) {
            if (length > 64) {
                return zza(bArr, 0, length);
            }
            long j = ((long) (length << 1)) - 7286425919675154353L;
            long jZzc = zzc(bArr, 0) * (-7286425919675154353L);
            long jZzc2 = zzc(bArr, 8);
            int i = length + 0;
            long jZzc3 = zzc(bArr, i - 8) * j;
            long jRotateRight = Long.rotateRight(jZzc + jZzc2, 43) + Long.rotateRight(jZzc3, 30) + (zzc(bArr, i - 16) * (-7286425919675154353L));
            long jZza = zza(jRotateRight, jZzc + Long.rotateRight(jZzc2 - 7286425919675154353L, 18) + jZzc3, j);
            long jZzc4 = zzc(bArr, 16) * j;
            long jZzc5 = zzc(bArr, 24);
            long jZzc6 = (jRotateRight + zzc(bArr, i - 32)) * j;
            return zza(Long.rotateRight(jZzc4 + jZzc5, 43) + Long.rotateRight(jZzc6, 30) + ((jZza + zzc(bArr, i - 24)) * j), jZzc4 + Long.rotateRight(jZzc5 + jZzc, 18) + jZzc6, j);
        }
        if (length > 16) {
            long j2 = ((long) (length << 1)) - 7286425919675154353L;
            long jZzc7 = zzc(bArr, 0) * (-5435081209227447693L);
            long jZzc8 = zzc(bArr, 8);
            int i2 = length + 0;
            long jZzc9 = zzc(bArr, i2 - 8) * j2;
            return zza((zzc(bArr, i2 - 16) * (-7286425919675154353L)) + Long.rotateRight(jZzc7 + jZzc8, 43) + Long.rotateRight(jZzc9, 30), jZzc7 + Long.rotateRight(jZzc8 - 7286425919675154353L, 18) + jZzc9, j2);
        }
        if (length >= 8) {
            long j3 = ((long) (length << 1)) - 7286425919675154353L;
            long jZzc10 = zzc(bArr, 0) - 7286425919675154353L;
            long jZzc11 = zzc(bArr, (length + 0) - 8);
            return zza((Long.rotateRight(jZzc11, 37) * j3) + jZzc10, (Long.rotateRight(jZzc10, 25) + jZzc11) * j3, j3);
        }
        if (length >= 4) {
            return zza(((long) length) + ((((long) zzb(bArr, 0)) & 4294967295L) << 3), ((long) zzb(bArr, (length + 0) - 4)) & 4294967295L, ((long) (length << 1)) - 7286425919675154353L);
        }
        if (length <= 0) {
            return -7286425919675154353L;
        }
        byte b = bArr[0];
        byte b2 = bArr[(length >> 1) + 0];
        long j4 = (((long) (length + ((bArr[(length - 1) + 0] & Constants.UNKNOWN) << 2))) * (-4348849565147123417L)) ^ (((long) ((b & Constants.UNKNOWN) + ((b2 & Constants.UNKNOWN) << 8))) * (-7286425919675154353L));
        return (j4 ^ (j4 >>> 47)) * (-7286425919675154353L);
    }
}
