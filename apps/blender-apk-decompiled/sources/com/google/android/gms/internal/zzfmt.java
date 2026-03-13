package com.google.android.gms.internal;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class zzfmt extends zzflm<zzfmt> implements Cloneable {
    private int zzpzp = -1;
    private int zzpzq = 0;

    public zzfmt() {
        this.zzpvl = null;
        this.zzpnr = -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Override // com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzbo, reason: merged with bridge method [inline-methods] */
    public final zzfmt zza(zzflj zzfljVar) throws IOException {
        while (true) {
            int iZzcxx = zzfljVar.zzcxx();
            if (iZzcxx == 0) {
                return this;
            }
            if (iZzcxx == 8) {
                zzfljVar.getPosition();
                int iZzcya = zzfljVar.zzcya();
                switch (iZzcya) {
                    case -1:
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                        this.zzpzp = iZzcya;
                        break;
                    default:
                        StringBuilder sb = new StringBuilder(43);
                        sb.append(iZzcya);
                        sb.append(" is not a valid enum NetworkType");
                        throw new IllegalArgumentException(sb.toString());
                }
            } else if (iZzcxx == 16) {
                try {
                    int iZzcya2 = zzfljVar.zzcya();
                    if (iZzcya2 != 100) {
                        switch (iZzcya2) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 14:
                            case 15:
                            case 16:
                                break;
                            default:
                                StringBuilder sb2 = new StringBuilder(45);
                                sb2.append(iZzcya2);
                                sb2.append(" is not a valid enum MobileSubtype");
                                throw new IllegalArgumentException(sb2.toString());
                        }
                    }
                    this.zzpzq = iZzcya2;
                } catch (IllegalArgumentException unused) {
                    zzfljVar.zzmw(zzfljVar.getPosition());
                    zza(zzfljVar, iZzcxx);
                }
            } else if (!super.zza(zzfljVar, iZzcxx)) {
                return this;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzddh, reason: merged with bridge method [inline-methods] */
    public zzfmt clone() {
        try {
            return (zzfmt) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof zzfmt)) {
            return false;
        }
        zzfmt zzfmtVar = (zzfmt) obj;
        if (this.zzpzp == zzfmtVar.zzpzp && this.zzpzq == zzfmtVar.zzpzq) {
            return (this.zzpvl == null || this.zzpvl.isEmpty()) ? zzfmtVar.zzpvl == null || zzfmtVar.zzpvl.isEmpty() : this.zzpvl.equals(zzfmtVar.zzpvl);
        }
        return false;
    }

    public final int hashCode() {
        return ((((((getClass().getName().hashCode() + 527) * 31) + this.zzpzp) * 31) + this.zzpzq) * 31) + ((this.zzpvl == null || this.zzpvl.isEmpty()) ? 0 : this.zzpvl.hashCode());
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    public final void zza(zzflk zzflkVar) throws IOException {
        if (this.zzpzp != -1) {
            zzflkVar.zzad(1, this.zzpzp);
        }
        if (this.zzpzq != 0) {
            zzflkVar.zzad(2, this.zzpzq);
        }
        super.zza(zzflkVar);
    }

    @Override // com.google.android.gms.internal.zzflm
    /* JADX INFO: renamed from: zzdck */
    public final /* synthetic */ zzflm clone() throws CloneNotSupportedException {
        return (zzfmt) clone();
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzdcl */
    public final /* synthetic */ zzfls clone() throws CloneNotSupportedException {
        return (zzfmt) clone();
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    protected final int zzq() {
        int iZzq = super.zzq();
        if (this.zzpzp != -1) {
            iZzq += zzflk.zzag(1, this.zzpzp);
        }
        return this.zzpzq != 0 ? iZzq + zzflk.zzag(2, this.zzpzq) : iZzq;
    }
}
