package com.google.android.gms.internal;

import com.google.android.gms.internal.zzfhu;
import java.io.IOException;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class zzfko {
    private static final zzfko zzpta = new zzfko(0, new int[0], new Object[0], false);
    private int count;
    private boolean zzpnq;
    private int zzppi;
    private int[] zzptb;
    private Object[] zzptc;

    private zzfko() {
        this(0, new int[8], new Object[8], true);
    }

    private zzfko(int i, int[] iArr, Object[] objArr, boolean z) {
        this.zzppi = -1;
        this.count = i;
        this.zzptb = iArr;
        this.zzptc = objArr;
        this.zzpnq = z;
    }

    static zzfko zzb(zzfko zzfkoVar, zzfko zzfkoVar2) {
        int i = zzfkoVar.count + zzfkoVar2.count;
        int[] iArrCopyOf = Arrays.copyOf(zzfkoVar.zzptb, i);
        System.arraycopy(zzfkoVar2.zzptb, 0, iArrCopyOf, zzfkoVar.count, zzfkoVar2.count);
        Object[] objArrCopyOf = Arrays.copyOf(zzfkoVar.zzptc, i);
        System.arraycopy(zzfkoVar2.zzptc, 0, objArrCopyOf, zzfkoVar.count, zzfkoVar2.count);
        return new zzfko(i, iArrCopyOf, objArrCopyOf, true);
    }

    private void zzc(int i, Object obj) {
        zzdbr();
        if (this.count == this.zzptb.length) {
            int i2 = this.count + (this.count < 4 ? 8 : this.count >> 1);
            this.zzptb = Arrays.copyOf(this.zzptb, i2);
            this.zzptc = Arrays.copyOf(this.zzptc, i2);
        }
        this.zzptb[this.count] = i;
        this.zzptc[this.count] = obj;
        this.count++;
    }

    private final void zzdbr() {
        if (!this.zzpnq) {
            throw new UnsupportedOperationException();
        }
    }

    public static zzfko zzdca() {
        return zzpta;
    }

    static zzfko zzdcb() {
        return new zzfko();
    }

    public final boolean equals(Object obj) {
        boolean z;
        boolean z2;
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof zzfko)) {
            return false;
        }
        zzfko zzfkoVar = (zzfko) obj;
        if (this.count == zzfkoVar.count) {
            int[] iArr = this.zzptb;
            int[] iArr2 = zzfkoVar.zzptb;
            int i = this.count;
            int i2 = 0;
            while (true) {
                if (i2 >= i) {
                    z = true;
                    break;
                }
                if (iArr[i2] != iArr2[i2]) {
                    z = false;
                    break;
                }
                i2++;
            }
            if (z) {
                Object[] objArr = this.zzptc;
                Object[] objArr2 = zzfkoVar.zzptc;
                int i3 = this.count;
                int i4 = 0;
                while (true) {
                    if (i4 >= i3) {
                        z2 = true;
                        break;
                    }
                    if (!objArr[i4].equals(objArr2[i4])) {
                        z2 = false;
                        break;
                    }
                    i4++;
                }
                if (z2) {
                    return true;
                }
            }
        }
        return false;
    }

    public final int hashCode() {
        return ((((this.count + 527) * 31) + Arrays.hashCode(this.zzptb)) * 31) + Arrays.deepHashCode(this.zzptc);
    }

    public final void zza(zzfhg zzfhgVar) throws IOException {
        for (int i = 0; i < this.count; i++) {
            int i2 = this.zzptb[i];
            int i3 = i2 >>> 3;
            int i4 = i2 & 7;
            if (i4 != 5) {
                switch (i4) {
                    case 0:
                        zzfhgVar.zza(i3, ((Long) this.zzptc[i]).longValue());
                        break;
                    case 1:
                        zzfhgVar.zzb(i3, ((Long) this.zzptc[i]).longValue());
                        break;
                    case 2:
                        zzfhgVar.zza(i3, (zzfgs) this.zzptc[i]);
                        break;
                    case 3:
                        zzfhgVar.zzac(i3, 3);
                        ((zzfko) this.zzptc[i]).zza(zzfhgVar);
                        zzfhgVar.zzac(i3, 4);
                        break;
                    default:
                        throw zzfie.zzdaj();
                }
            } else {
                zzfhgVar.zzaf(i3, ((Integer) this.zzptc[i]).intValue());
            }
        }
    }

    final void zza(zzfli zzfliVar) {
        if (zzfliVar.zzcyz() == zzfhu.zzg.zzpqg) {
            for (int i = this.count - 1; i >= 0; i--) {
                zzfliVar.zzb(this.zzptb[i] >>> 3, this.zzptc[i]);
            }
            return;
        }
        for (int i2 = 0; i2 < this.count; i2++) {
            zzfliVar.zzb(this.zzptb[i2] >>> 3, this.zzptc[i2]);
        }
    }

    final boolean zzb(int i, zzfhb zzfhbVar) throws IOException {
        int iZzcxx;
        zzdbr();
        int i2 = i >>> 3;
        switch (i & 7) {
            case 0:
                zzc(i, Long.valueOf(zzfhbVar.zzcxz()));
                return true;
            case 1:
                zzc(i, Long.valueOf(zzfhbVar.zzcyb()));
                return true;
            case 2:
                zzc(i, zzfhbVar.zzcyf());
                return true;
            case 3:
                zzfko zzfkoVar = new zzfko();
                do {
                    iZzcxx = zzfhbVar.zzcxx();
                    if (iZzcxx != 0) {
                    }
                    zzfhbVar.zzlf((i2 << 3) | 4);
                    zzc(i, zzfkoVar);
                    return true;
                } while (zzfkoVar.zzb(iZzcxx, zzfhbVar));
                zzfhbVar.zzlf((i2 << 3) | 4);
                zzc(i, zzfkoVar);
                return true;
            case 4:
                return false;
            case 5:
                zzc(i, Integer.valueOf(zzfhbVar.zzcyc()));
                return true;
            default:
                throw zzfie.zzdaj();
        }
    }

    public final void zzbkr() {
        this.zzpnq = false;
    }

    final void zzd(StringBuilder sb, int i) {
        for (int i2 = 0; i2 < this.count; i2++) {
            zzfjf.zzb(sb, i, String.valueOf(this.zzptb[i2] >>> 3), this.zzptc[i2]);
        }
    }

    public final int zzdcc() {
        int i = this.zzppi;
        if (i != -1) {
            return i;
        }
        int iZzd = 0;
        for (int i2 = 0; i2 < this.count; i2++) {
            iZzd += zzfhg.zzd(this.zzptb[i2] >>> 3, (zzfgs) this.zzptc[i2]);
        }
        this.zzppi = iZzd;
        return iZzd;
    }

    public final int zzhs() {
        int iZzai;
        int i = this.zzppi;
        if (i != -1) {
            return i;
        }
        int i2 = 0;
        for (int i3 = 0; i3 < this.count; i3++) {
            int i4 = this.zzptb[i3];
            int i5 = i4 >>> 3;
            int i6 = i4 & 7;
            if (i6 != 5) {
                switch (i6) {
                    case 0:
                        iZzai = zzfhg.zzd(i5, ((Long) this.zzptc[i3]).longValue());
                        break;
                    case 1:
                        iZzai = zzfhg.zze(i5, ((Long) this.zzptc[i3]).longValue());
                        break;
                    case 2:
                        iZzai = zzfhg.zzc(i5, (zzfgs) this.zzptc[i3]);
                        break;
                    case 3:
                        iZzai = (zzfhg.zzlw(i5) << 1) + ((zzfko) this.zzptc[i3]).zzhs();
                        break;
                    default:
                        throw new IllegalStateException(zzfie.zzdaj());
                }
            } else {
                iZzai = zzfhg.zzai(i5, ((Integer) this.zzptc[i3]).intValue());
            }
            i2 += iZzai;
        }
        this.zzppi = i2;
        return i2;
    }
}
