package com.google.android.gms.internal;

import android.support.v7.widget.ActivityChooserView;
import com.flurry.android.Constants;
import java.io.IOException;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
final class zzfhd extends zzfhb {
    private final byte[] buffer;
    private final boolean immutable;
    private int limit;
    private int pos;
    private int zzpoh;
    private int zzpoi;
    private int zzpoj;
    private int zzpok;

    private zzfhd(byte[] bArr, int i, int i2, boolean z) {
        super();
        this.zzpok = ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        this.buffer = bArr;
        this.limit = i2 + i;
        this.pos = i;
        this.zzpoi = this.pos;
        this.immutable = z;
    }

    private final long zzcyr() throws IOException {
        int i;
        long j;
        long j2;
        long j3;
        int i2;
        int i3 = this.pos;
        if (this.limit != i3) {
            byte[] bArr = this.buffer;
            int i4 = i3 + 1;
            byte b = bArr[i3];
            if (b >= 0) {
                this.pos = i4;
                return b;
            }
            if (this.limit - i4 >= 9) {
                int i5 = i4 + 1;
                int i6 = b ^ (bArr[i4] << 7);
                if (i6 >= 0) {
                    int i7 = i5 + 1;
                    int i8 = i6 ^ (bArr[i5] << 14);
                    if (i8 >= 0) {
                        long j4 = i8 ^ 16256;
                        i = i7;
                        j = j4;
                    } else {
                        i5 = i7 + 1;
                        int i9 = i8 ^ (bArr[i7] << 21);
                        if (i9 < 0) {
                            i2 = i9 ^ (-2080896);
                        } else {
                            long j5 = i9;
                            i = i5 + 1;
                            long j6 = (((long) bArr[i5]) << 28) ^ j5;
                            if (j6 >= 0) {
                                j3 = 266354560;
                            } else {
                                int i10 = i + 1;
                                long j7 = j6 ^ (((long) bArr[i]) << 35);
                                if (j7 < 0) {
                                    j2 = -34093383808L;
                                } else {
                                    i = i10 + 1;
                                    j6 = j7 ^ (((long) bArr[i10]) << 42);
                                    if (j6 >= 0) {
                                        j3 = 4363953127296L;
                                    } else {
                                        i10 = i + 1;
                                        j7 = j6 ^ (((long) bArr[i]) << 49);
                                        if (j7 < 0) {
                                            j2 = -558586000294016L;
                                        } else {
                                            i = i10 + 1;
                                            long j8 = (j7 ^ (((long) bArr[i10]) << 56)) ^ 71499008037633920L;
                                            if (j8 < 0) {
                                                i10 = i + 1;
                                                if (bArr[i] >= 0) {
                                                    j = j8;
                                                    i = i10;
                                                }
                                            } else {
                                                j = j8;
                                            }
                                        }
                                    }
                                }
                                j = j2 ^ j7;
                                i = i10;
                            }
                            j = j6 ^ j3;
                        }
                    }
                    this.pos = i;
                    return j;
                }
                i2 = i6 ^ (-128);
                j = i2;
                i = i5;
                this.pos = i;
                return j;
            }
        }
        return zzcyn();
    }

    private final int zzcys() throws IOException {
        int i = this.pos;
        if (this.limit - i < 4) {
            throw zzfie.zzdae();
        }
        byte[] bArr = this.buffer;
        this.pos = i + 4;
        return (bArr[i] & Constants.UNKNOWN) | ((bArr[i + 1] & Constants.UNKNOWN) << 8) | ((bArr[i + 2] & Constants.UNKNOWN) << 16) | ((bArr[i + 3] & Constants.UNKNOWN) << 24);
    }

    private final long zzcyt() throws IOException {
        int i = this.pos;
        if (this.limit - i < 8) {
            throw zzfie.zzdae();
        }
        byte[] bArr = this.buffer;
        this.pos = i + 8;
        return ((((long) bArr[i + 7]) & 255) << 56) | (((long) bArr[i]) & 255) | ((((long) bArr[i + 1]) & 255) << 8) | ((((long) bArr[i + 2]) & 255) << 16) | ((((long) bArr[i + 3]) & 255) << 24) | ((((long) bArr[i + 4]) & 255) << 32) | ((((long) bArr[i + 5]) & 255) << 40) | ((((long) bArr[i + 6]) & 255) << 48);
    }

    private final void zzcyu() {
        this.limit += this.zzpoh;
        int i = this.limit - this.zzpoi;
        if (i <= this.zzpok) {
            this.zzpoh = 0;
        } else {
            this.zzpoh = i - this.zzpok;
            this.limit -= this.zzpoh;
        }
    }

    private final byte zzcyv() throws IOException {
        if (this.pos == this.limit) {
            throw zzfie.zzdae();
        }
        byte[] bArr = this.buffer;
        int i = this.pos;
        this.pos = i + 1;
        return bArr[i];
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(zzcyt());
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(zzcys());
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final String readString() throws IOException {
        int iZzcym = zzcym();
        if (iZzcym > 0 && iZzcym <= this.limit - this.pos) {
            String str = new String(this.buffer, this.pos, iZzcym, zzfhz.UTF_8);
            this.pos += iZzcym;
            return str;
        }
        if (iZzcym == 0) {
            return "";
        }
        if (iZzcym < 0) {
            throw zzfie.zzdaf();
        }
        throw zzfie.zzdae();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final <T extends zzfhu<T, ?>> T zza(T t, zzfhm zzfhmVar) throws IOException {
        int iZzcym = zzcym();
        if (this.zzpoc >= this.zzpod) {
            throw zzfie.zzdak();
        }
        int iZzli = zzli(iZzcym);
        this.zzpoc++;
        T t2 = (T) zzfhu.zza(t, this, zzfhmVar);
        zzlf(0);
        this.zzpoc--;
        zzlj(iZzli);
        return t2;
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final void zza(zzfjd zzfjdVar, zzfhm zzfhmVar) throws IOException {
        int iZzcym = zzcym();
        if (this.zzpoc >= this.zzpod) {
            throw zzfie.zzdak();
        }
        int iZzli = zzli(iZzcym);
        this.zzpoc++;
        zzfjdVar.zzb(this, zzfhmVar);
        zzlf(0);
        this.zzpoc--;
        zzlj(iZzli);
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final int zzcxx() throws IOException {
        if (zzcyp()) {
            this.zzpoj = 0;
            return 0;
        }
        this.zzpoj = zzcym();
        if ((this.zzpoj >>> 3) != 0) {
            return this.zzpoj;
        }
        throw zzfie.zzdah();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final long zzcxy() throws IOException {
        return zzcyr();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final long zzcxz() throws IOException {
        return zzcyr();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final int zzcya() throws IOException {
        return zzcym();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final long zzcyb() throws IOException {
        return zzcyt();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final int zzcyc() throws IOException {
        return zzcys();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final boolean zzcyd() throws IOException {
        return zzcyr() != 0;
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final String zzcye() throws IOException {
        int iZzcym = zzcym();
        if (iZzcym <= 0 || iZzcym > this.limit - this.pos) {
            if (iZzcym == 0) {
                return "";
            }
            if (iZzcym <= 0) {
                throw zzfie.zzdaf();
            }
            throw zzfie.zzdae();
        }
        if (!zzfks.zzl(this.buffer, this.pos, this.pos + iZzcym)) {
            throw zzfie.zzdam();
        }
        int i = this.pos;
        this.pos += iZzcym;
        return new String(this.buffer, i, iZzcym, zzfhz.UTF_8);
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final zzfgs zzcyf() throws IOException {
        byte[] bArrCopyOfRange;
        int iZzcym = zzcym();
        if (iZzcym > 0 && iZzcym <= this.limit - this.pos) {
            zzfgs zzfgsVarZzf = zzfgs.zzf(this.buffer, this.pos, iZzcym);
            this.pos += iZzcym;
            return zzfgsVarZzf;
        }
        if (iZzcym == 0) {
            return zzfgs.zzpnw;
        }
        if (iZzcym > 0 && iZzcym <= this.limit - this.pos) {
            int i = this.pos;
            this.pos += iZzcym;
            bArrCopyOfRange = Arrays.copyOfRange(this.buffer, i, this.pos);
        } else {
            if (iZzcym > 0) {
                throw zzfie.zzdae();
            }
            if (iZzcym != 0) {
                throw zzfie.zzdaf();
            }
            bArrCopyOfRange = zzfhz.EMPTY_BYTE_ARRAY;
        }
        return zzfgs.zzba(bArrCopyOfRange);
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final int zzcyg() throws IOException {
        return zzcym();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final int zzcyh() throws IOException {
        return zzcym();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final int zzcyi() throws IOException {
        return zzcys();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final long zzcyj() throws IOException {
        return zzcyt();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final int zzcyk() throws IOException {
        return zzll(zzcym());
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final long zzcyl() throws IOException {
        return zzct(zzcyr());
    }

    /* JADX WARN: Code restructure failed: missing block: B:31:0x0068, code lost:
    
        if (r1[r2] >= 0) goto L32;
     */
    @Override // com.google.android.gms.internal.zzfhb
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final int zzcym() throws java.io.IOException {
        /*
            r5 = this;
            int r0 = r5.pos
            int r1 = r5.limit
            if (r1 == r0) goto L6d
            byte[] r1 = r5.buffer
            int r2 = r0 + 1
            r0 = r1[r0]
            if (r0 < 0) goto L11
            r5.pos = r2
            return r0
        L11:
            int r3 = r5.limit
            int r3 = r3 - r2
            r4 = 9
            if (r3 < r4) goto L6d
            int r3 = r2 + 1
            r2 = r1[r2]
            int r2 = r2 << 7
            r0 = r0 ^ r2
            if (r0 >= 0) goto L24
            r0 = r0 ^ (-128(0xffffffffffffff80, float:NaN))
            goto L6a
        L24:
            int r2 = r3 + 1
            r3 = r1[r3]
            int r3 = r3 << 14
            r0 = r0 ^ r3
            if (r0 < 0) goto L31
            r0 = r0 ^ 16256(0x3f80, float:2.278E-41)
        L2f:
            r3 = r2
            goto L6a
        L31:
            int r3 = r2 + 1
            r2 = r1[r2]
            int r2 = r2 << 21
            r0 = r0 ^ r2
            if (r0 >= 0) goto L3f
            r1 = -2080896(0xffffffffffe03f80, float:NaN)
            r0 = r0 ^ r1
            goto L6a
        L3f:
            int r2 = r3 + 1
            r3 = r1[r3]
            int r4 = r3 << 28
            r0 = r0 ^ r4
            r4 = 266354560(0xfe03f80, float:2.2112565E-29)
            r0 = r0 ^ r4
            if (r3 >= 0) goto L2f
            int r3 = r2 + 1
            r2 = r1[r2]
            if (r2 >= 0) goto L6a
            int r2 = r3 + 1
            r3 = r1[r3]
            if (r3 >= 0) goto L2f
            int r3 = r2 + 1
            r2 = r1[r2]
            if (r2 >= 0) goto L6a
            int r2 = r3 + 1
            r3 = r1[r3]
            if (r3 >= 0) goto L2f
            int r3 = r2 + 1
            r1 = r1[r2]
            if (r1 < 0) goto L6d
        L6a:
            r5.pos = r3
            return r0
        L6d:
            long r0 = r5.zzcyn()
            int r5 = (int) r0
            return r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.internal.zzfhd.zzcym():int");
    }

    @Override // com.google.android.gms.internal.zzfhb
    final long zzcyn() throws IOException {
        long j = 0;
        for (int i = 0; i < 64; i += 7) {
            byte bZzcyv = zzcyv();
            j |= ((long) (bZzcyv & 127)) << i;
            if ((bZzcyv & 128) == 0) {
                return j;
            }
        }
        throw zzfie.zzdag();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final int zzcyo() {
        if (this.zzpok == Integer.MAX_VALUE) {
            return -1;
        }
        return this.zzpok - zzcyq();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final boolean zzcyp() throws IOException {
        return this.pos == this.limit;
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final int zzcyq() {
        return this.pos - this.zzpoi;
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final void zzlf(int i) throws zzfie {
        if (this.zzpoj != i) {
            throw zzfie.zzdai();
        }
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final boolean zzlg(int i) throws IOException {
        int iZzcxx;
        int i2 = 0;
        switch (i & 7) {
            case 0:
                if (this.limit - this.pos < 10) {
                    while (i2 < 10) {
                        if (zzcyv() < 0) {
                            i2++;
                        }
                    }
                    throw zzfie.zzdag();
                }
                while (i2 < 10) {
                    byte[] bArr = this.buffer;
                    int i3 = this.pos;
                    this.pos = i3 + 1;
                    if (bArr[i3] < 0) {
                        i2++;
                    }
                }
                throw zzfie.zzdag();
                return true;
            case 1:
                zzlk(8);
                return true;
            case 2:
                zzlk(zzcym());
                return true;
            case 3:
                break;
            case 4:
                return false;
            case 5:
                zzlk(4);
                return true;
            default:
                throw zzfie.zzdaj();
        }
        do {
            iZzcxx = zzcxx();
            if (iZzcxx != 0) {
            }
            zzlf(((i >>> 3) << 3) | 4);
            return true;
        } while (zzlg(iZzcxx));
        zzlf(((i >>> 3) << 3) | 4);
        return true;
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final int zzli(int i) throws zzfie {
        if (i < 0) {
            throw zzfie.zzdaf();
        }
        int iZzcyq = i + zzcyq();
        int i2 = this.zzpok;
        if (iZzcyq > i2) {
            throw zzfie.zzdae();
        }
        this.zzpok = iZzcyq;
        zzcyu();
        return i2;
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final void zzlj(int i) {
        this.zzpok = i;
        zzcyu();
    }

    @Override // com.google.android.gms.internal.zzfhb
    public final void zzlk(int i) throws IOException {
        if (i >= 0 && i <= this.limit - this.pos) {
            this.pos += i;
        } else {
            if (i >= 0) {
                throw zzfie.zzdae();
            }
            throw zzfie.zzdaf();
        }
    }
}
