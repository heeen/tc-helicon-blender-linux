package com.google.android.gms.internal;

import com.google.android.gms.internal.zzfhu;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class zzfmg extends zzfhu<zzfmg, zza> implements zzfje {
    private static volatile zzfjl<zzfmg> zzbbm;
    private static final zzfmg zzpxv;
    private int zzmjb;
    private int zzpxs;
    private String zzpxt = "";
    private zzfid<zzfgp> zzpxu = zzczs();

    public static final class zza extends zzfhu.zza<zzfmg, zza> implements zzfje {
        private zza() {
            super(zzfmg.zzpxv);
        }

        /* synthetic */ zza(zzfmh zzfmhVar) {
            this();
        }
    }

    static {
        zzfmg zzfmgVar = new zzfmg();
        zzpxv = zzfmgVar;
        zzfmgVar.zza(zzfhu.zzg.zzppw, (Object) null, (Object) null);
        zzfmgVar.zzpph.zzbkr();
    }

    private zzfmg() {
    }

    public static zzfmg zzdcu() {
        return zzpxv;
    }

    public final int getCode() {
        return this.zzpxs;
    }

    public final String getMessage() {
        return this.zzpxt;
    }

    @Override // com.google.android.gms.internal.zzfhu
    protected final Object zza(int i, Object obj, Object obj2) {
        zzfmh zzfmhVar = null;
        switch (zzfmh.zzbbk[i - 1]) {
            case 1:
                return new zzfmg();
            case 2:
                return zzpxv;
            case 3:
                this.zzpxu.zzbkr();
                return null;
            case 4:
                return new zza(zzfmhVar);
            case 5:
                zzfhu.zzh zzhVar = (zzfhu.zzh) obj;
                zzfmg zzfmgVar = (zzfmg) obj2;
                this.zzpxs = zzhVar.zza(this.zzpxs != 0, this.zzpxs, zzfmgVar.zzpxs != 0, zzfmgVar.zzpxs);
                this.zzpxt = zzhVar.zza(!this.zzpxt.isEmpty(), this.zzpxt, true ^ zzfmgVar.zzpxt.isEmpty(), zzfmgVar.zzpxt);
                this.zzpxu = zzhVar.zza(this.zzpxu, zzfmgVar.zzpxu);
                if (zzhVar == zzfhu.zzf.zzppq) {
                    this.zzmjb |= zzfmgVar.zzmjb;
                }
                return this;
            case 6:
                zzfhb zzfhbVar = (zzfhb) obj;
                zzfhm zzfhmVar = (zzfhm) obj2;
                if (zzfhmVar == null) {
                    throw new NullPointerException();
                }
                while (!z) {
                    try {
                        int iZzcxx = zzfhbVar.zzcxx();
                        if (iZzcxx != 0) {
                            if (iZzcxx == 8) {
                                this.zzpxs = zzfhbVar.zzcya();
                            } else if (iZzcxx == 18) {
                                this.zzpxt = zzfhbVar.zzcye();
                            } else if (iZzcxx == 26) {
                                if (!this.zzpxu.zzcxk()) {
                                    zzfid<zzfgp> zzfidVar = this.zzpxu;
                                    int size = zzfidVar.size();
                                    this.zzpxu = zzfidVar.zzmo(size == 0 ? 10 : size << 1);
                                }
                                this.zzpxu.add((zzfgp) zzfhbVar.zza(zzfgp.zzcxo(), zzfhmVar));
                            } else if (!zza(iZzcxx, zzfhbVar)) {
                            }
                        }
                        z = true;
                    } catch (zzfie e) {
                        throw new RuntimeException(e.zzi(this));
                    } catch (IOException e2) {
                        throw new RuntimeException(new zzfie(e2.getMessage()).zzi(this));
                    }
                }
                break;
                break;
            case 7:
                break;
            case 8:
                if (zzbbm == null) {
                    synchronized (zzfmg.class) {
                        if (zzbbm == null) {
                            zzbbm = new zzfhu.zzb(zzpxv);
                        }
                        break;
                    }
                }
                return zzbbm;
            case 9:
                return (byte) 1;
            case 10:
                return null;
            default:
                throw new UnsupportedOperationException();
        }
        return zzpxv;
    }

    @Override // com.google.android.gms.internal.zzfhu, com.google.android.gms.internal.zzfjc
    public final void zza(zzfhg zzfhgVar) throws IOException {
        if (this.zzpxs != 0) {
            zzfhgVar.zzad(1, this.zzpxs);
        }
        if (!this.zzpxt.isEmpty()) {
            zzfhgVar.zzp(2, this.zzpxt);
        }
        for (int i = 0; i < this.zzpxu.size(); i++) {
            zzfhgVar.zza(3, this.zzpxu.get(i));
        }
        this.zzpph.zza(zzfhgVar);
    }

    @Override // com.google.android.gms.internal.zzfhu, com.google.android.gms.internal.zzfjc
    public final int zzhs() {
        int i = this.zzppi;
        if (i != -1) {
            return i;
        }
        int iZzag = this.zzpxs != 0 ? zzfhg.zzag(1, this.zzpxs) + 0 : 0;
        if (!this.zzpxt.isEmpty()) {
            iZzag += zzfhg.zzq(2, this.zzpxt);
        }
        for (int i2 = 0; i2 < this.zzpxu.size(); i2++) {
            iZzag += zzfhg.zzc(3, this.zzpxu.get(i2));
        }
        int iZzhs = iZzag + this.zzpph.zzhs();
        this.zzppi = iZzhs;
        return iZzhs;
    }
}
