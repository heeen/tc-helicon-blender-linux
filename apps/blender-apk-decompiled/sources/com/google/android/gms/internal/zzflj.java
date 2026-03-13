package com.google.android.gms.internal;

import android.support.v7.widget.ActivityChooserView;
import com.flurry.android.Constants;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class zzflj {
    private final byte[] buffer;
    private int zzpoc;
    private int zzpoh;
    private int zzpoj;
    private final int zzpom;
    private final int zzpvi;
    private int zzpvj;
    private int zzpvk;
    private int zzpok = ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
    private int zzpod = 64;
    private int zzpoe = 67108864;

    private zzflj(byte[] bArr, int i, int i2) {
        this.buffer = bArr;
        this.zzpvi = i;
        int i3 = i2 + i;
        this.zzpvj = i3;
        this.zzpom = i3;
        this.zzpvk = i;
    }

    public static zzflj zzbe(byte[] bArr) {
        return zzo(bArr, 0, bArr.length);
    }

    private final void zzcyu() {
        this.zzpvj += this.zzpoh;
        int i = this.zzpvj;
        if (i <= this.zzpok) {
            this.zzpoh = 0;
        } else {
            this.zzpoh = i - this.zzpok;
            this.zzpvj -= this.zzpoh;
        }
    }

    private final byte zzcyv() throws IOException {
        if (this.zzpvk == this.zzpvj) {
            throw zzflr.zzdcn();
        }
        byte[] bArr = this.buffer;
        int i = this.zzpvk;
        this.zzpvk = i + 1;
        return bArr[i];
    }

    private final void zzlk(int i) throws IOException {
        if (i < 0) {
            throw zzflr.zzdco();
        }
        if (this.zzpvk + i > this.zzpok) {
            zzlk(this.zzpok - this.zzpvk);
            throw zzflr.zzdcn();
        }
        if (i > this.zzpvj - this.zzpvk) {
            throw zzflr.zzdcn();
        }
        this.zzpvk += i;
    }

    public static zzflj zzo(byte[] bArr, int i, int i2) {
        return new zzflj(bArr, 0, i2);
    }

    public final int getPosition() {
        return this.zzpvk - this.zzpvi;
    }

    public final byte[] readBytes() throws IOException {
        int iZzcym = zzcym();
        if (iZzcym < 0) {
            throw zzflr.zzdco();
        }
        if (iZzcym == 0) {
            return zzflv.zzpwe;
        }
        if (iZzcym > this.zzpvj - this.zzpvk) {
            throw zzflr.zzdcn();
        }
        byte[] bArr = new byte[iZzcym];
        System.arraycopy(this.buffer, this.zzpvk, bArr, 0, iZzcym);
        this.zzpvk += iZzcym;
        return bArr;
    }

    public final String readString() throws IOException {
        int iZzcym = zzcym();
        if (iZzcym < 0) {
            throw zzflr.zzdco();
        }
        if (iZzcym > this.zzpvj - this.zzpvk) {
            throw zzflr.zzdcn();
        }
        String str = new String(this.buffer, this.zzpvk, iZzcym, zzflq.UTF_8);
        this.zzpvk += iZzcym;
        return str;
    }

    public final void zza(zzfls zzflsVar) throws IOException {
        int iZzcym = zzcym();
        if (this.zzpoc >= this.zzpod) {
            throw zzflr.zzdcq();
        }
        int iZzli = zzli(iZzcym);
        this.zzpoc++;
        zzflsVar.zza(this);
        zzlf(0);
        this.zzpoc--;
        zzlj(iZzli);
    }

    public final void zza(zzfls zzflsVar, int i) throws IOException {
        if (this.zzpoc >= this.zzpod) {
            throw zzflr.zzdcq();
        }
        this.zzpoc++;
        zzflsVar.zza(this);
        zzlf((i << 3) | 4);
        this.zzpoc--;
    }

    public final byte[] zzao(int i, int i2) {
        if (i2 == 0) {
            return zzflv.zzpwe;
        }
        byte[] bArr = new byte[i2];
        System.arraycopy(this.buffer, this.zzpvi + i, bArr, 0, i2);
        return bArr;
    }

    final void zzap(int i, int i2) {
        if (i > this.zzpvk - this.zzpvi) {
            int i3 = this.zzpvk - this.zzpvi;
            StringBuilder sb = new StringBuilder(50);
            sb.append("Position ");
            sb.append(i);
            sb.append(" is beyond current ");
            sb.append(i3);
            throw new IllegalArgumentException(sb.toString());
        }
        if (i >= 0) {
            this.zzpvk = this.zzpvi + i;
            this.zzpoj = i2;
        } else {
            StringBuilder sb2 = new StringBuilder(24);
            sb2.append("Bad position ");
            sb2.append(i);
            throw new IllegalArgumentException(sb2.toString());
        }
    }

    public final int zzcxx() throws IOException {
        if (this.zzpvk == this.zzpvj) {
            this.zzpoj = 0;
            return 0;
        }
        this.zzpoj = zzcym();
        if (this.zzpoj != 0) {
            return this.zzpoj;
        }
        throw new zzflr("Protocol message contained an invalid tag (zero).");
    }

    public final long zzcxz() throws IOException {
        return zzcyr();
    }

    public final int zzcya() throws IOException {
        return zzcym();
    }

    public final boolean zzcyd() throws IOException {
        return zzcym() != 0;
    }

    public final long zzcyl() throws IOException {
        long jZzcyr = zzcyr();
        return (-(jZzcyr & 1)) ^ (jZzcyr >>> 1);
    }

    public final int zzcym() throws IOException {
        int i;
        byte bZzcyv = zzcyv();
        if (bZzcyv >= 0) {
            return bZzcyv;
        }
        int i2 = bZzcyv & 127;
        byte bZzcyv2 = zzcyv();
        if (bZzcyv2 >= 0) {
            i = bZzcyv2 << 7;
        } else {
            i2 |= (bZzcyv2 & 127) << 7;
            byte bZzcyv3 = zzcyv();
            if (bZzcyv3 >= 0) {
                i = bZzcyv3 << 14;
            } else {
                i2 |= (bZzcyv3 & 127) << 14;
                byte bZzcyv4 = zzcyv();
                if (bZzcyv4 < 0) {
                    int i3 = i2 | ((bZzcyv4 & 127) << 21);
                    byte bZzcyv5 = zzcyv();
                    int i4 = i3 | (bZzcyv5 << 28);
                    if (bZzcyv5 >= 0) {
                        return i4;
                    }
                    for (int i5 = 0; i5 < 5; i5++) {
                        if (zzcyv() >= 0) {
                            return i4;
                        }
                    }
                    throw zzflr.zzdcp();
                }
                i = bZzcyv4 << 21;
            }
        }
        return i | i2;
    }

    public final int zzcyo() {
        if (this.zzpok == Integer.MAX_VALUE) {
            return -1;
        }
        return this.zzpok - this.zzpvk;
    }

    public final long zzcyr() throws IOException {
        long j = 0;
        for (int i = 0; i < 64; i += 7) {
            byte bZzcyv = zzcyv();
            j |= ((long) (bZzcyv & 127)) << i;
            if ((bZzcyv & 128) == 0) {
                return j;
            }
        }
        throw zzflr.zzdcp();
    }

    public final int zzcys() throws IOException {
        byte bZzcyv = zzcyv();
        byte bZzcyv2 = zzcyv();
        byte bZzcyv3 = zzcyv();
        byte bZzcyv4 = zzcyv();
        return ((bZzcyv4 & Constants.UNKNOWN) << 24) | (bZzcyv & Constants.UNKNOWN) | ((bZzcyv2 & Constants.UNKNOWN) << 8) | ((bZzcyv3 & Constants.UNKNOWN) << 16);
    }

    public final long zzcyt() throws IOException {
        byte bZzcyv = zzcyv();
        return ((((long) zzcyv()) & 255) << 8) | (((long) bZzcyv) & 255) | ((((long) zzcyv()) & 255) << 16) | ((((long) zzcyv()) & 255) << 24) | ((((long) zzcyv()) & 255) << 32) | ((((long) zzcyv()) & 255) << 40) | ((((long) zzcyv()) & 255) << 48) | ((((long) zzcyv()) & 255) << 56);
    }

    public final void zzlf(int i) throws zzflr {
        if (this.zzpoj != i) {
            throw new zzflr("Protocol message end-group tag did not match expected tag.");
        }
    }

    public final boolean zzlg(int i) throws IOException {
        int iZzcxx;
        switch (i & 7) {
            case 0:
                zzcym();
                return true;
            case 1:
                zzcyt();
                return true;
            case 2:
                zzlk(zzcym());
                return true;
            case 3:
                break;
            case 4:
                return false;
            case 5:
                zzcys();
                return true;
            default:
                throw new zzflr("Protocol message tag had invalid wire type.");
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

    public final int zzli(int i) throws zzflr {
        if (i < 0) {
            throw zzflr.zzdco();
        }
        int i2 = i + this.zzpvk;
        int i3 = this.zzpok;
        if (i2 > i3) {
            throw zzflr.zzdcn();
        }
        this.zzpok = i2;
        zzcyu();
        return i3;
    }

    public final void zzlj(int i) {
        this.zzpok = i;
        zzcyu();
    }

    public final void zzmw(int i) {
        zzap(i, this.zzpoj);
    }
}
