package com.google.android.gms.internal;

import com.google.android.gms.internal.zzfhu;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class zzfkk extends zzfhu<zzfkk, zza> implements zzfje {
    private static volatile zzfjl<zzfkk> zzbbm;
    private static final zzfkk zzpsy;
    private long zzpsw;
    private int zzpsx;

    public static final class zza extends zzfhu.zza<zzfkk, zza> implements zzfje {
        private zza() {
            super(zzfkk.zzpsy);
        }

        /* synthetic */ zza(zzfkl zzfklVar) {
            this();
        }

        public final zza zzdh(long j) {
            zzczv();
            ((zzfkk) this.zzppl).zzdg(j);
            return this;
        }

        public final zza zzmt(int i) {
            zzczv();
            ((zzfkk) this.zzppl).setNanos(i);
            return this;
        }
    }

    static {
        zzfkk zzfkkVar = new zzfkk();
        zzpsy = zzfkkVar;
        zzfkkVar.zza(zzfhu.zzg.zzppw, (Object) null, (Object) null);
        zzfkkVar.zzpph.zzbkr();
    }

    private zzfkk() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void setNanos(int i) {
        this.zzpsx = i;
    }

    public static zza zzdbw() {
        return (zza) ((zzfhu.zza) zzpsy.zza(zzfhu.zzg.zzppy, (Object) null, (Object) null));
    }

    public static zzfkk zzdbx() {
        return zzpsy;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void zzdg(long j) {
        this.zzpsw = j;
    }

    public final int getNanos() {
        return this.zzpsx;
    }

    public final long getSeconds() {
        return this.zzpsw;
    }

    @Override // com.google.android.gms.internal.zzfhu
    protected final Object zza(int i, Object obj, Object obj2) {
        boolean zZzb;
        zzfkl zzfklVar = null;
        switch (zzfkl.zzbbk[i - 1]) {
            case 1:
                return new zzfkk();
            case 2:
                return zzpsy;
            case 3:
                return null;
            case 4:
                return new zza(zzfklVar);
            case 5:
                zzfhu.zzh zzhVar = (zzfhu.zzh) obj;
                zzfkk zzfkkVar = (zzfkk) obj2;
                this.zzpsw = zzhVar.zza(this.zzpsw != 0, this.zzpsw, zzfkkVar.zzpsw != 0, zzfkkVar.zzpsw);
                this.zzpsx = zzhVar.zza(this.zzpsx != 0, this.zzpsx, zzfkkVar.zzpsx != 0, zzfkkVar.zzpsx);
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
                            if (iZzcxx == 8) {
                                this.zzpsw = zzfhbVar.zzcxz();
                            } else if (iZzcxx != 16) {
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
                                this.zzpsx = zzfhbVar.zzcya();
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
                    synchronized (zzfkk.class) {
                        if (zzbbm == null) {
                            zzbbm = new zzfhu.zzb(zzpsy);
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
        return zzpsy;
    }

    @Override // com.google.android.gms.internal.zzfhu, com.google.android.gms.internal.zzfjc
    public final void zza(zzfhg zzfhgVar) throws IOException {
        if (this.zzpsw != 0) {
            zzfhgVar.zza(1, this.zzpsw);
        }
        if (this.zzpsx != 0) {
            zzfhgVar.zzad(2, this.zzpsx);
        }
        this.zzpph.zza(zzfhgVar);
    }

    @Override // com.google.android.gms.internal.zzfhu, com.google.android.gms.internal.zzfjc
    public final int zzhs() {
        int i = this.zzppi;
        if (i != -1) {
            return i;
        }
        int iZzc = this.zzpsw != 0 ? 0 + zzfhg.zzc(1, this.zzpsw) : 0;
        if (this.zzpsx != 0) {
            iZzc += zzfhg.zzag(2, this.zzpsx);
        }
        int iZzhs = iZzc + this.zzpph.zzhs();
        this.zzppi = iZzhs;
        return iZzhs;
    }
}
