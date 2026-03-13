package com.google.android.gms.internal;

import com.google.android.gms.internal.zzfhu;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class zzfgp extends zzfhu<zzfgp, zza> implements zzfje {
    private static volatile zzfjl<zzfgp> zzbbm;
    private static final zzfgp zzpnv;
    private String zzmid = "";
    private zzfgs zzmie = zzfgs.zzpnw;

    public static final class zza extends zzfhu.zza<zzfgp, zza> implements zzfje {
        private zza() {
            super(zzfgp.zzpnv);
        }

        /* synthetic */ zza(zzfgq zzfgqVar) {
            this();
        }
    }

    static {
        zzfgp zzfgpVar = new zzfgp();
        zzpnv = zzfgpVar;
        zzfgpVar.zza(zzfhu.zzg.zzppw, (Object) null, (Object) null);
        zzfgpVar.zzpph.zzbkr();
    }

    private zzfgp() {
    }

    public static zzfgp zzcxo() {
        return zzpnv;
    }

    @Override // com.google.android.gms.internal.zzfhu
    protected final Object zza(int i, Object obj, Object obj2) {
        boolean zZzb;
        zzfgq zzfgqVar = null;
        switch (zzfgq.zzbbk[i - 1]) {
            case 1:
                return new zzfgp();
            case 2:
                return zzpnv;
            case 3:
                return null;
            case 4:
                return new zza(zzfgqVar);
            case 5:
                zzfhu.zzh zzhVar = (zzfhu.zzh) obj;
                zzfgp zzfgpVar = (zzfgp) obj2;
                this.zzmid = zzhVar.zza(!this.zzmid.isEmpty(), this.zzmid, !zzfgpVar.zzmid.isEmpty(), zzfgpVar.zzmid);
                this.zzmie = zzhVar.zza(this.zzmie != zzfgs.zzpnw, this.zzmie, zzfgpVar.zzmie != zzfgs.zzpnw, zzfgpVar.zzmie);
                return this;
            case 6:
                zzfhb zzfhbVar = (zzfhb) obj;
                if (((zzfhm) obj2) == null) {
                    throw new NullPointerException();
                }
                boolean z = false;
                while (!z) {
                    try {
                        int iZzcxx = zzfhbVar.zzcxx();
                        if (iZzcxx != 0) {
                            if (iZzcxx == 10) {
                                this.zzmid = zzfhbVar.zzcye();
                            } else if (iZzcxx != 18) {
                                if ((iZzcxx & 7) == 4) {
                                    zZzb = false;
                                } else {
                                    if (this.zzpph == zzfko.zzdca()) {
                                        this.zzpph = zzfko.zzdcb();
                                    }
                                    zZzb = this.zzpph.zzb(iZzcxx, zzfhbVar);
                                }
                                if (!zZzb) {
                                }
                            } else {
                                this.zzmie = zzfhbVar.zzcyf();
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
                    synchronized (zzfgp.class) {
                        if (zzbbm == null) {
                            zzbbm = new zzfhu.zzb(zzpnv);
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
        return zzpnv;
    }

    @Override // com.google.android.gms.internal.zzfhu, com.google.android.gms.internal.zzfjc
    public final void zza(zzfhg zzfhgVar) throws IOException {
        if (!this.zzmid.isEmpty()) {
            zzfhgVar.zzp(1, this.zzmid);
        }
        if (!this.zzmie.isEmpty()) {
            zzfhgVar.zza(2, this.zzmie);
        }
        this.zzpph.zza(zzfhgVar);
    }

    @Override // com.google.android.gms.internal.zzfhu, com.google.android.gms.internal.zzfjc
    public final int zzhs() {
        int i = this.zzppi;
        if (i != -1) {
            return i;
        }
        int iZzq = this.zzmid.isEmpty() ? 0 : 0 + zzfhg.zzq(1, this.zzmid);
        if (!this.zzmie.isEmpty()) {
            iZzq += zzfhg.zzc(2, this.zzmie);
        }
        int iZzhs = iZzq + this.zzpph.zzhs();
        this.zzppi = iZzhs;
        return iZzhs;
    }
}
