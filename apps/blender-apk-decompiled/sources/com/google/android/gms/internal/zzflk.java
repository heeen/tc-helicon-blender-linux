package com.google.android.gms.internal;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;

/* JADX INFO: loaded from: classes.dex */
public final class zzflk {
    private final ByteBuffer buffer;

    private zzflk(ByteBuffer byteBuffer) {
        this.buffer = byteBuffer;
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    private zzflk(byte[] bArr, int i, int i2) {
        this(ByteBuffer.wrap(bArr, i, i2));
    }

    private static int zza(CharSequence charSequence, byte[] bArr, int i, int i2) {
        int i3;
        int i4;
        char cCharAt;
        int length = charSequence.length();
        int i5 = i2 + i;
        int i6 = 0;
        while (i6 < length && (i4 = i6 + i) < i5 && (cCharAt = charSequence.charAt(i6)) < 128) {
            bArr[i4] = (byte) cCharAt;
            i6++;
        }
        if (i6 == length) {
            return i + length;
        }
        int i7 = i + i6;
        while (i6 < length) {
            char cCharAt2 = charSequence.charAt(i6);
            if (cCharAt2 >= 128 || i7 >= i5) {
                if (cCharAt2 < 2048 && i7 <= i5 - 2) {
                    int i8 = i7 + 1;
                    bArr[i7] = (byte) ((cCharAt2 >>> 6) | 960);
                    i7 = i8 + 1;
                    bArr[i8] = (byte) ((cCharAt2 & '?') | 128);
                } else {
                    if ((cCharAt2 >= 55296 && 57343 >= cCharAt2) || i7 > i5 - 3) {
                        if (i7 > i5 - 4) {
                            StringBuilder sb = new StringBuilder(37);
                            sb.append("Failed writing ");
                            sb.append(cCharAt2);
                            sb.append(" at index ");
                            sb.append(i7);
                            throw new ArrayIndexOutOfBoundsException(sb.toString());
                        }
                        int i9 = i6 + 1;
                        if (i9 != charSequence.length()) {
                            char cCharAt3 = charSequence.charAt(i9);
                            if (Character.isSurrogatePair(cCharAt2, cCharAt3)) {
                                int codePoint = Character.toCodePoint(cCharAt2, cCharAt3);
                                int i10 = i7 + 1;
                                bArr[i7] = (byte) ((codePoint >>> 18) | 240);
                                int i11 = i10 + 1;
                                bArr[i10] = (byte) (((codePoint >>> 12) & 63) | 128);
                                int i12 = i11 + 1;
                                bArr[i11] = (byte) (((codePoint >>> 6) & 63) | 128);
                                i7 = i12 + 1;
                                bArr[i12] = (byte) ((codePoint & 63) | 128);
                                i6 = i9;
                            } else {
                                i6 = i9;
                            }
                        }
                        StringBuilder sb2 = new StringBuilder(39);
                        sb2.append("Unpaired surrogate at index ");
                        sb2.append(i6 - 1);
                        throw new IllegalArgumentException(sb2.toString());
                    }
                    int i13 = i7 + 1;
                    bArr[i7] = (byte) ((cCharAt2 >>> '\f') | 480);
                    int i14 = i13 + 1;
                    bArr[i13] = (byte) (((cCharAt2 >>> 6) & 63) | 128);
                    i3 = i14 + 1;
                    bArr[i14] = (byte) ((cCharAt2 & '?') | 128);
                }
                i6++;
            } else {
                i3 = i7 + 1;
                bArr[i7] = (byte) cCharAt2;
            }
            i7 = i3;
            i6++;
        }
        return i7;
    }

    private static void zza(CharSequence charSequence, ByteBuffer byteBuffer) {
        if (byteBuffer.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (!byteBuffer.hasArray()) {
            zzb(charSequence, byteBuffer);
            return;
        }
        try {
            byteBuffer.position(zza(charSequence, byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.remaining()) - byteBuffer.arrayOffset());
        } catch (ArrayIndexOutOfBoundsException e) {
            BufferOverflowException bufferOverflowException = new BufferOverflowException();
            bufferOverflowException.initCause(e);
            throw bufferOverflowException;
        }
    }

    public static int zzag(int i, int i2) {
        return zzlw(i) + zzlx(i2);
    }

    public static int zzb(int i, zzfls zzflsVar) {
        int iZzlw = zzlw(i);
        int iZzhs = zzflsVar.zzhs();
        return iZzlw + zzmf(iZzhs) + iZzhs;
    }

    private static void zzb(CharSequence charSequence, ByteBuffer byteBuffer) {
        int i;
        int length = charSequence.length();
        int i2 = 0;
        while (i2 < length) {
            char cCharAt = charSequence.charAt(i2);
            int i3 = cCharAt;
            if (cCharAt >= 128) {
                if (cCharAt < 2048) {
                    i = (cCharAt >>> 6) | 960;
                } else {
                    if (cCharAt >= 55296 && 57343 >= cCharAt) {
                        int i4 = i2 + 1;
                        if (i4 != charSequence.length()) {
                            char cCharAt2 = charSequence.charAt(i4);
                            if (Character.isSurrogatePair(cCharAt, cCharAt2)) {
                                int codePoint = Character.toCodePoint(cCharAt, cCharAt2);
                                byteBuffer.put((byte) ((codePoint >>> 18) | 240));
                                byteBuffer.put((byte) (((codePoint >>> 12) & 63) | 128));
                                byteBuffer.put((byte) (((codePoint >>> 6) & 63) | 128));
                                byteBuffer.put((byte) ((codePoint & 63) | 128));
                                i2 = i4;
                            } else {
                                i2 = i4;
                            }
                        }
                        StringBuilder sb = new StringBuilder(39);
                        sb.append("Unpaired surrogate at index ");
                        sb.append(i2 - 1);
                        throw new IllegalArgumentException(sb.toString());
                    }
                    byteBuffer.put((byte) ((cCharAt >>> '\f') | 480));
                    i = ((cCharAt >>> 6) & 63) | 128;
                }
                byteBuffer.put((byte) i);
                i3 = (cCharAt & '?') | 128;
                byteBuffer.put((byte) i3);
            } else {
                byteBuffer.put((byte) i3);
            }
            i2++;
        }
    }

    public static zzflk zzbf(byte[] bArr) {
        return zzp(bArr, 0, bArr.length);
    }

    public static int zzbg(byte[] bArr) {
        return zzmf(bArr.length) + bArr.length;
    }

    public static int zzc(int i, long j) {
        return zzlw(i) + zzdj(j);
    }

    public static int zzd(int i, byte[] bArr) {
        return zzlw(i) + zzbg(bArr);
    }

    private static int zzd(CharSequence charSequence) {
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
                                StringBuilder sb = new StringBuilder(39);
                                sb.append("Unpaired surrogate at index ");
                                sb.append(i2);
                                throw new IllegalArgumentException(sb.toString());
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
        StringBuilder sb2 = new StringBuilder(54);
        sb2.append("UTF-8 length does not fit in int: ");
        sb2.append(j);
        throw new IllegalArgumentException(sb2.toString());
    }

    private static long zzdc(long j) {
        return (j >> 63) ^ (j << 1);
    }

    private final void zzdi(long j) throws IOException {
        while (((-128) & j) != 0) {
            zzmx((((int) j) & 127) | 128);
            j >>>= 7;
        }
        zzmx((int) j);
    }

    public static int zzdj(long j) {
        if (((-128) & j) == 0) {
            return 1;
        }
        if (((-16384) & j) == 0) {
            return 2;
        }
        if (((-2097152) & j) == 0) {
            return 3;
        }
        if (((-268435456) & j) == 0) {
            return 4;
        }
        if (((-34359738368L) & j) == 0) {
            return 5;
        }
        if (((-4398046511104L) & j) == 0) {
            return 6;
        }
        if (((-562949953421312L) & j) == 0) {
            return 7;
        }
        if (((-72057594037927936L) & j) == 0) {
            return 8;
        }
        return (j & Long.MIN_VALUE) == 0 ? 9 : 10;
    }

    private final void zzdk(long j) throws IOException {
        if (this.buffer.remaining() < 8) {
            throw new zzfll(this.buffer.position(), this.buffer.limit());
        }
        this.buffer.putLong(j);
    }

    public static int zzh(int i, long j) {
        return zzlw(i) + zzdj(zzdc(j));
    }

    public static int zzlw(int i) {
        return zzmf(i << 3);
    }

    public static int zzlx(int i) {
        if (i >= 0) {
            return zzmf(i);
        }
        return 10;
    }

    public static int zzme(int i) {
        return (i >> 31) ^ (i << 1);
    }

    public static int zzmf(int i) {
        if ((i & (-128)) == 0) {
            return 1;
        }
        if ((i & (-16384)) == 0) {
            return 2;
        }
        if (((-2097152) & i) == 0) {
            return 3;
        }
        return (i & (-268435456)) == 0 ? 4 : 5;
    }

    private final void zzmx(int i) throws IOException {
        byte b = (byte) i;
        if (!this.buffer.hasRemaining()) {
            throw new zzfll(this.buffer.position(), this.buffer.limit());
        }
        this.buffer.put(b);
    }

    public static zzflk zzp(byte[] bArr, int i, int i2) {
        return new zzflk(bArr, 0, i2);
    }

    public static int zzq(int i, String str) {
        return zzlw(i) + zztx(str);
    }

    public static int zztx(String str) {
        int iZzd = zzd(str);
        return zzmf(iZzd) + iZzd;
    }

    public final void zza(int i, double d) throws IOException {
        zzac(i, 1);
        zzdk(Double.doubleToLongBits(d));
    }

    public final void zza(int i, long j) throws IOException {
        zzac(i, 0);
        zzdi(j);
    }

    public final void zza(int i, zzfls zzflsVar) throws IOException {
        zzac(i, 2);
        zzb(zzflsVar);
    }

    public final void zzac(int i, int i2) throws IOException {
        zzmy((i << 3) | i2);
    }

    public final void zzad(int i, int i2) throws IOException {
        zzac(i, 0);
        if (i2 >= 0) {
            zzmy(i2);
        } else {
            zzdi(i2);
        }
    }

    public final void zzb(int i, long j) throws IOException {
        zzac(i, 1);
        zzdk(j);
    }

    public final void zzb(zzfls zzflsVar) throws IOException {
        zzmy(zzflsVar.zzdcr());
        zzflsVar.zza(this);
    }

    public final void zzbh(byte[] bArr) throws IOException {
        int length = bArr.length;
        if (this.buffer.remaining() < length) {
            throw new zzfll(this.buffer.position(), this.buffer.limit());
        }
        this.buffer.put(bArr, 0, length);
    }

    public final void zzc(int i, byte[] bArr) throws IOException {
        zzac(i, 2);
        zzmy(bArr.length);
        zzbh(bArr);
    }

    public final void zzcyx() {
        if (this.buffer.remaining() != 0) {
            throw new IllegalStateException(String.format("Did not write as much data as expected, %s bytes remaining.", Integer.valueOf(this.buffer.remaining())));
        }
    }

    public final void zzd(int i, float f) throws IOException {
        zzac(i, 5);
        int iFloatToIntBits = Float.floatToIntBits(f);
        if (this.buffer.remaining() < 4) {
            throw new zzfll(this.buffer.position(), this.buffer.limit());
        }
        this.buffer.putInt(iFloatToIntBits);
    }

    public final void zzf(int i, long j) throws IOException {
        zzac(i, 0);
        zzdi(j);
    }

    public final void zzg(int i, long j) throws IOException {
        zzac(i, 0);
        zzdi(zzdc(j));
    }

    public final void zzl(int i, boolean z) throws IOException {
        zzac(i, 0);
        byte b = z ? (byte) 1 : (byte) 0;
        if (!this.buffer.hasRemaining()) {
            throw new zzfll(this.buffer.position(), this.buffer.limit());
        }
        this.buffer.put(b);
    }

    public final void zzmy(int i) throws IOException {
        while ((i & (-128)) != 0) {
            zzmx((i & 127) | 128);
            i >>>= 7;
        }
        zzmx(i);
    }

    public final void zzp(int i, String str) throws IOException {
        zzac(i, 2);
        try {
            int iZzmf = zzmf(str.length());
            if (iZzmf != zzmf(str.length() * 3)) {
                zzmy(zzd(str));
                zza(str, this.buffer);
                return;
            }
            int iPosition = this.buffer.position();
            if (this.buffer.remaining() < iZzmf) {
                throw new zzfll(iPosition + iZzmf, this.buffer.limit());
            }
            this.buffer.position(iPosition + iZzmf);
            zza(str, this.buffer);
            int iPosition2 = this.buffer.position();
            this.buffer.position(iPosition);
            zzmy((iPosition2 - iPosition) - iZzmf);
            this.buffer.position(iPosition2);
        } catch (BufferOverflowException e) {
            zzfll zzfllVar = new zzfll(this.buffer.position(), this.buffer.limit());
            zzfllVar.initCause(e);
            throw zzfllVar;
        }
    }
}
