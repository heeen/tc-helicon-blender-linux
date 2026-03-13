package com.google.android.gms.internal;

import android.support.v7.widget.helper.ItemTouchHelper;
import java.io.IOException;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class zzfmr extends zzflm<zzfmr> implements Cloneable {
    public long zzpyu = 0;
    public long zzpyv = 0;
    private long zzpyw = 0;
    private String tag = "";
    private int zzpyx = 0;
    private int zzaky = 0;
    private boolean zznet = false;
    private zzfms[] zzpyy = zzfms.zzddf();
    private byte[] zzpyz = zzflv.zzpwe;
    private zzfmp zzpza = null;
    public byte[] zzpzb = zzflv.zzpwe;
    private String zzpzc = "";
    private String zzpzd = "";
    private zzfmo zzpze = null;
    private String zzpzf = "";
    public long zzpzg = 180000;
    private zzfmq zzpzh = null;
    public byte[] zzpzi = zzflv.zzpwe;
    private String zzpzj = "";
    private int zzpzk = 0;
    private int[] zzpzl = zzflv.zzpvy;
    private long zzpzm = 0;
    private zzfmt zzorb = null;
    private boolean zzpzn = false;

    public zzfmr() {
        this.zzpvl = null;
        this.zzpnr = -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Override // com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzbn, reason: merged with bridge method [inline-methods] */
    public final zzfmr zza(zzflj zzfljVar) throws IOException {
        zzfls zzflsVar;
        int iZzcya;
        while (true) {
            int iZzcxx = zzfljVar.zzcxx();
            switch (iZzcxx) {
                case 0:
                    return this;
                case 8:
                    this.zzpyu = zzfljVar.zzcxz();
                    continue;
                case 18:
                    this.tag = zzfljVar.readString();
                    continue;
                case 26:
                    int iZzb = zzflv.zzb(zzfljVar, 26);
                    int length = this.zzpyy == null ? 0 : this.zzpyy.length;
                    zzfms[] zzfmsVarArr = new zzfms[iZzb + length];
                    if (length != 0) {
                        System.arraycopy(this.zzpyy, 0, zzfmsVarArr, 0, length);
                    }
                    while (length < zzfmsVarArr.length - 1) {
                        zzfmsVarArr[length] = new zzfms();
                        zzfljVar.zza(zzfmsVarArr[length]);
                        zzfljVar.zzcxx();
                        length++;
                    }
                    zzfmsVarArr[length] = new zzfms();
                    zzfljVar.zza(zzfmsVarArr[length]);
                    this.zzpyy = zzfmsVarArr;
                    continue;
                case 34:
                    this.zzpyz = zzfljVar.readBytes();
                    continue;
                case 50:
                    this.zzpzb = zzfljVar.readBytes();
                    continue;
                case 58:
                    if (this.zzpze == null) {
                        this.zzpze = new zzfmo();
                    }
                    zzflsVar = this.zzpze;
                    break;
                case 66:
                    this.zzpzc = zzfljVar.readString();
                    continue;
                case 74:
                    if (this.zzpza == null) {
                        this.zzpza = new zzfmp();
                    }
                    zzflsVar = this.zzpza;
                    break;
                case 80:
                    this.zznet = zzfljVar.zzcyd();
                    continue;
                case 88:
                    this.zzpyx = zzfljVar.zzcya();
                    continue;
                case 96:
                    this.zzaky = zzfljVar.zzcya();
                    continue;
                case 106:
                    this.zzpzd = zzfljVar.readString();
                    continue;
                case 114:
                    this.zzpzf = zzfljVar.readString();
                    continue;
                case 120:
                    this.zzpzg = zzfljVar.zzcyl();
                    continue;
                case 130:
                    if (this.zzpzh == null) {
                        this.zzpzh = new zzfmq();
                    }
                    zzflsVar = this.zzpzh;
                    break;
                case 136:
                    this.zzpyv = zzfljVar.zzcxz();
                    continue;
                case 146:
                    this.zzpzi = zzfljVar.readBytes();
                    continue;
                case 152:
                    int position = zzfljVar.getPosition();
                    try {
                        iZzcya = zzfljVar.zzcya();
                    } catch (IllegalArgumentException unused) {
                        zzfljVar.zzmw(position);
                        zza(zzfljVar, iZzcxx);
                    }
                    switch (iZzcya) {
                        case 0:
                        case 1:
                        case 2:
                            this.zzpzk = iZzcya;
                            continue;
                            continue;
                        default:
                            StringBuilder sb = new StringBuilder(45);
                            sb.append(iZzcya);
                            sb.append(" is not a valid enum InternalEvent");
                            throw new IllegalArgumentException(sb.toString());
                    }
                    zzfljVar.zzmw(position);
                    zza(zzfljVar, iZzcxx);
                    break;
                case 160:
                    int iZzb2 = zzflv.zzb(zzfljVar, 160);
                    int length2 = this.zzpzl == null ? 0 : this.zzpzl.length;
                    int[] iArr = new int[iZzb2 + length2];
                    if (length2 != 0) {
                        System.arraycopy(this.zzpzl, 0, iArr, 0, length2);
                    }
                    while (length2 < iArr.length - 1) {
                        iArr[length2] = zzfljVar.zzcya();
                        zzfljVar.zzcxx();
                        length2++;
                    }
                    iArr[length2] = zzfljVar.zzcya();
                    this.zzpzl = iArr;
                    continue;
                case 162:
                    int iZzli = zzfljVar.zzli(zzfljVar.zzcym());
                    int position2 = zzfljVar.getPosition();
                    int i = 0;
                    while (zzfljVar.zzcyo() > 0) {
                        zzfljVar.zzcya();
                        i++;
                    }
                    zzfljVar.zzmw(position2);
                    int length3 = this.zzpzl == null ? 0 : this.zzpzl.length;
                    int[] iArr2 = new int[i + length3];
                    if (length3 != 0) {
                        System.arraycopy(this.zzpzl, 0, iArr2, 0, length3);
                    }
                    while (length3 < iArr2.length) {
                        iArr2[length3] = zzfljVar.zzcya();
                        length3++;
                    }
                    this.zzpzl = iArr2;
                    zzfljVar.zzlj(iZzli);
                    continue;
                case 168:
                    this.zzpyw = zzfljVar.zzcxz();
                    continue;
                case 176:
                    this.zzpzm = zzfljVar.zzcxz();
                    continue;
                case 186:
                    if (this.zzorb == null) {
                        this.zzorb = new zzfmt();
                    }
                    zzflsVar = this.zzorb;
                    break;
                case 194:
                    this.zzpzj = zzfljVar.readString();
                    continue;
                case ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION /* 200 */:
                    this.zzpzn = zzfljVar.zzcyd();
                    continue;
                default:
                    if (!super.zza(zzfljVar, iZzcxx)) {
                        return this;
                    }
                    continue;
                    break;
            }
            zzfljVar.zza(zzflsVar);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzdde, reason: merged with bridge method [inline-methods] */
    public final zzfmr clone() {
        try {
            zzfmr zzfmrVar = (zzfmr) super.clone();
            if (this.zzpyy != null && this.zzpyy.length > 0) {
                zzfmrVar.zzpyy = new zzfms[this.zzpyy.length];
                for (int i = 0; i < this.zzpyy.length; i++) {
                    if (this.zzpyy[i] != null) {
                        zzfmrVar.zzpyy[i] = (zzfms) this.zzpyy[i].clone();
                    }
                }
            }
            if (this.zzpza != null) {
                zzfmrVar.zzpza = (zzfmp) this.zzpza.clone();
            }
            if (this.zzpze != null) {
                zzfmrVar.zzpze = (zzfmo) this.zzpze.clone();
            }
            if (this.zzpzh != null) {
                zzfmrVar.zzpzh = (zzfmq) this.zzpzh.clone();
            }
            if (this.zzpzl != null && this.zzpzl.length > 0) {
                zzfmrVar.zzpzl = (int[]) this.zzpzl.clone();
            }
            if (this.zzorb != null) {
                zzfmrVar.zzorb = (zzfmt) this.zzorb.clone();
            }
            return zzfmrVar;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof zzfmr)) {
            return false;
        }
        zzfmr zzfmrVar = (zzfmr) obj;
        if (this.zzpyu != zzfmrVar.zzpyu || this.zzpyv != zzfmrVar.zzpyv || this.zzpyw != zzfmrVar.zzpyw) {
            return false;
        }
        if (this.tag == null) {
            if (zzfmrVar.tag != null) {
                return false;
            }
        } else if (!this.tag.equals(zzfmrVar.tag)) {
            return false;
        }
        if (this.zzpyx != zzfmrVar.zzpyx || this.zzaky != zzfmrVar.zzaky || this.zznet != zzfmrVar.zznet || !zzflq.equals(this.zzpyy, zzfmrVar.zzpyy) || !Arrays.equals(this.zzpyz, zzfmrVar.zzpyz)) {
            return false;
        }
        if (this.zzpza == null) {
            if (zzfmrVar.zzpza != null) {
                return false;
            }
        } else if (!this.zzpza.equals(zzfmrVar.zzpza)) {
            return false;
        }
        if (!Arrays.equals(this.zzpzb, zzfmrVar.zzpzb)) {
            return false;
        }
        if (this.zzpzc == null) {
            if (zzfmrVar.zzpzc != null) {
                return false;
            }
        } else if (!this.zzpzc.equals(zzfmrVar.zzpzc)) {
            return false;
        }
        if (this.zzpzd == null) {
            if (zzfmrVar.zzpzd != null) {
                return false;
            }
        } else if (!this.zzpzd.equals(zzfmrVar.zzpzd)) {
            return false;
        }
        if (this.zzpze == null) {
            if (zzfmrVar.zzpze != null) {
                return false;
            }
        } else if (!this.zzpze.equals(zzfmrVar.zzpze)) {
            return false;
        }
        if (this.zzpzf == null) {
            if (zzfmrVar.zzpzf != null) {
                return false;
            }
        } else if (!this.zzpzf.equals(zzfmrVar.zzpzf)) {
            return false;
        }
        if (this.zzpzg != zzfmrVar.zzpzg) {
            return false;
        }
        if (this.zzpzh == null) {
            if (zzfmrVar.zzpzh != null) {
                return false;
            }
        } else if (!this.zzpzh.equals(zzfmrVar.zzpzh)) {
            return false;
        }
        if (!Arrays.equals(this.zzpzi, zzfmrVar.zzpzi)) {
            return false;
        }
        if (this.zzpzj == null) {
            if (zzfmrVar.zzpzj != null) {
                return false;
            }
        } else if (!this.zzpzj.equals(zzfmrVar.zzpzj)) {
            return false;
        }
        if (this.zzpzk != zzfmrVar.zzpzk || !zzflq.equals(this.zzpzl, zzfmrVar.zzpzl) || this.zzpzm != zzfmrVar.zzpzm) {
            return false;
        }
        if (this.zzorb == null) {
            if (zzfmrVar.zzorb != null) {
                return false;
            }
        } else if (!this.zzorb.equals(zzfmrVar.zzorb)) {
            return false;
        }
        if (this.zzpzn != zzfmrVar.zzpzn) {
            return false;
        }
        return (this.zzpvl == null || this.zzpvl.isEmpty()) ? zzfmrVar.zzpvl == null || zzfmrVar.zzpvl.isEmpty() : this.zzpvl.equals(zzfmrVar.zzpvl);
    }

    public final int hashCode() {
        int iHashCode = 0;
        int iHashCode2 = ((((((((((((((((((getClass().getName().hashCode() + 527) * 31) + ((int) (this.zzpyu ^ (this.zzpyu >>> 32)))) * 31) + ((int) (this.zzpyv ^ (this.zzpyv >>> 32)))) * 31) + ((int) (this.zzpyw ^ (this.zzpyw >>> 32)))) * 31) + (this.tag == null ? 0 : this.tag.hashCode())) * 31) + this.zzpyx) * 31) + this.zzaky) * 31) + (this.zznet ? 1231 : 1237)) * 31) + zzflq.hashCode(this.zzpyy)) * 31) + Arrays.hashCode(this.zzpyz);
        zzfmp zzfmpVar = this.zzpza;
        int iHashCode3 = (((((((iHashCode2 * 31) + (zzfmpVar == null ? 0 : zzfmpVar.hashCode())) * 31) + Arrays.hashCode(this.zzpzb)) * 31) + (this.zzpzc == null ? 0 : this.zzpzc.hashCode())) * 31) + (this.zzpzd == null ? 0 : this.zzpzd.hashCode());
        zzfmo zzfmoVar = this.zzpze;
        int iHashCode4 = (((((iHashCode3 * 31) + (zzfmoVar == null ? 0 : zzfmoVar.hashCode())) * 31) + (this.zzpzf == null ? 0 : this.zzpzf.hashCode())) * 31) + ((int) (this.zzpzg ^ (this.zzpzg >>> 32)));
        zzfmq zzfmqVar = this.zzpzh;
        int iHashCode5 = (((((((((((iHashCode4 * 31) + (zzfmqVar == null ? 0 : zzfmqVar.hashCode())) * 31) + Arrays.hashCode(this.zzpzi)) * 31) + (this.zzpzj == null ? 0 : this.zzpzj.hashCode())) * 31) + this.zzpzk) * 31) + zzflq.hashCode(this.zzpzl)) * 31) + ((int) (this.zzpzm ^ (this.zzpzm >>> 32)));
        zzfmt zzfmtVar = this.zzorb;
        int iHashCode6 = ((((iHashCode5 * 31) + (zzfmtVar == null ? 0 : zzfmtVar.hashCode())) * 31) + (this.zzpzn ? 1231 : 1237)) * 31;
        if (this.zzpvl != null && !this.zzpvl.isEmpty()) {
            iHashCode = this.zzpvl.hashCode();
        }
        return iHashCode6 + iHashCode;
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    public final void zza(zzflk zzflkVar) throws IOException {
        if (this.zzpyu != 0) {
            zzflkVar.zzf(1, this.zzpyu);
        }
        if (this.tag != null && !this.tag.equals("")) {
            zzflkVar.zzp(2, this.tag);
        }
        if (this.zzpyy != null && this.zzpyy.length > 0) {
            for (int i = 0; i < this.zzpyy.length; i++) {
                zzfms zzfmsVar = this.zzpyy[i];
                if (zzfmsVar != null) {
                    zzflkVar.zza(3, zzfmsVar);
                }
            }
        }
        if (!Arrays.equals(this.zzpyz, zzflv.zzpwe)) {
            zzflkVar.zzc(4, this.zzpyz);
        }
        if (!Arrays.equals(this.zzpzb, zzflv.zzpwe)) {
            zzflkVar.zzc(6, this.zzpzb);
        }
        if (this.zzpze != null) {
            zzflkVar.zza(7, this.zzpze);
        }
        if (this.zzpzc != null && !this.zzpzc.equals("")) {
            zzflkVar.zzp(8, this.zzpzc);
        }
        if (this.zzpza != null) {
            zzflkVar.zza(9, this.zzpza);
        }
        if (this.zznet) {
            zzflkVar.zzl(10, this.zznet);
        }
        if (this.zzpyx != 0) {
            zzflkVar.zzad(11, this.zzpyx);
        }
        if (this.zzaky != 0) {
            zzflkVar.zzad(12, this.zzaky);
        }
        if (this.zzpzd != null && !this.zzpzd.equals("")) {
            zzflkVar.zzp(13, this.zzpzd);
        }
        if (this.zzpzf != null && !this.zzpzf.equals("")) {
            zzflkVar.zzp(14, this.zzpzf);
        }
        if (this.zzpzg != 180000) {
            zzflkVar.zzg(15, this.zzpzg);
        }
        if (this.zzpzh != null) {
            zzflkVar.zza(16, this.zzpzh);
        }
        if (this.zzpyv != 0) {
            zzflkVar.zzf(17, this.zzpyv);
        }
        if (!Arrays.equals(this.zzpzi, zzflv.zzpwe)) {
            zzflkVar.zzc(18, this.zzpzi);
        }
        if (this.zzpzk != 0) {
            zzflkVar.zzad(19, this.zzpzk);
        }
        if (this.zzpzl != null && this.zzpzl.length > 0) {
            for (int i2 = 0; i2 < this.zzpzl.length; i2++) {
                zzflkVar.zzad(20, this.zzpzl[i2]);
            }
        }
        if (this.zzpyw != 0) {
            zzflkVar.zzf(21, this.zzpyw);
        }
        if (this.zzpzm != 0) {
            zzflkVar.zzf(22, this.zzpzm);
        }
        if (this.zzorb != null) {
            zzflkVar.zza(23, this.zzorb);
        }
        if (this.zzpzj != null && !this.zzpzj.equals("")) {
            zzflkVar.zzp(24, this.zzpzj);
        }
        if (this.zzpzn) {
            zzflkVar.zzl(25, this.zzpzn);
        }
        super.zza(zzflkVar);
    }

    @Override // com.google.android.gms.internal.zzflm
    /* JADX INFO: renamed from: zzdck */
    public final /* synthetic */ zzflm clone() throws CloneNotSupportedException {
        return (zzfmr) clone();
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzdcl */
    public final /* synthetic */ zzfls clone() throws CloneNotSupportedException {
        return (zzfmr) clone();
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    protected final int zzq() {
        int iZzq = super.zzq();
        if (this.zzpyu != 0) {
            iZzq += zzflk.zzc(1, this.zzpyu);
        }
        if (this.tag != null && !this.tag.equals("")) {
            iZzq += zzflk.zzq(2, this.tag);
        }
        if (this.zzpyy != null && this.zzpyy.length > 0) {
            int iZzb = iZzq;
            for (int i = 0; i < this.zzpyy.length; i++) {
                zzfms zzfmsVar = this.zzpyy[i];
                if (zzfmsVar != null) {
                    iZzb += zzflk.zzb(3, zzfmsVar);
                }
            }
            iZzq = iZzb;
        }
        if (!Arrays.equals(this.zzpyz, zzflv.zzpwe)) {
            iZzq += zzflk.zzd(4, this.zzpyz);
        }
        if (!Arrays.equals(this.zzpzb, zzflv.zzpwe)) {
            iZzq += zzflk.zzd(6, this.zzpzb);
        }
        if (this.zzpze != null) {
            iZzq += zzflk.zzb(7, this.zzpze);
        }
        if (this.zzpzc != null && !this.zzpzc.equals("")) {
            iZzq += zzflk.zzq(8, this.zzpzc);
        }
        if (this.zzpza != null) {
            iZzq += zzflk.zzb(9, this.zzpza);
        }
        if (this.zznet) {
            iZzq += zzflk.zzlw(10) + 1;
        }
        if (this.zzpyx != 0) {
            iZzq += zzflk.zzag(11, this.zzpyx);
        }
        if (this.zzaky != 0) {
            iZzq += zzflk.zzag(12, this.zzaky);
        }
        if (this.zzpzd != null && !this.zzpzd.equals("")) {
            iZzq += zzflk.zzq(13, this.zzpzd);
        }
        if (this.zzpzf != null && !this.zzpzf.equals("")) {
            iZzq += zzflk.zzq(14, this.zzpzf);
        }
        if (this.zzpzg != 180000) {
            iZzq += zzflk.zzh(15, this.zzpzg);
        }
        if (this.zzpzh != null) {
            iZzq += zzflk.zzb(16, this.zzpzh);
        }
        if (this.zzpyv != 0) {
            iZzq += zzflk.zzc(17, this.zzpyv);
        }
        if (!Arrays.equals(this.zzpzi, zzflv.zzpwe)) {
            iZzq += zzflk.zzd(18, this.zzpzi);
        }
        if (this.zzpzk != 0) {
            iZzq += zzflk.zzag(19, this.zzpzk);
        }
        if (this.zzpzl != null && this.zzpzl.length > 0) {
            int iZzlx = 0;
            for (int i2 = 0; i2 < this.zzpzl.length; i2++) {
                iZzlx += zzflk.zzlx(this.zzpzl[i2]);
            }
            iZzq = iZzq + iZzlx + (this.zzpzl.length * 2);
        }
        if (this.zzpyw != 0) {
            iZzq += zzflk.zzc(21, this.zzpyw);
        }
        if (this.zzpzm != 0) {
            iZzq += zzflk.zzc(22, this.zzpzm);
        }
        if (this.zzorb != null) {
            iZzq += zzflk.zzb(23, this.zzorb);
        }
        if (this.zzpzj != null && !this.zzpzj.equals("")) {
            iZzq += zzflk.zzq(24, this.zzpzj);
        }
        return this.zzpzn ? iZzq + zzflk.zzlw(25) + 1 : iZzq;
    }
}
