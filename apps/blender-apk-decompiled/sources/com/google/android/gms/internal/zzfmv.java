package com.google.android.gms.internal;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class zzfmv extends zzflm<zzfmv> {
    public String zzpzs = "";
    public String zzpzt = "";
    public long zzpzu = 0;
    public String zzpzv = "";
    public long zzpzw = 0;
    public long zzgoc = 0;
    public String zzpzx = "";
    public String zzpzy = "";
    public String zzpzz = "";
    public String zzqaa = "";
    public String zzqab = "";
    public int zzqac = 0;
    public zzfmu[] zzqad = zzfmu.zzddi();

    public zzfmv() {
        this.zzpvl = null;
        this.zzpnr = -1;
    }

    public static zzfmv zzbi(byte[] bArr) throws zzflr {
        return (zzfmv) zzfls.zza(new zzfmv(), bArr);
    }

    @Override // com.google.android.gms.internal.zzfls
    public final /* synthetic */ zzfls zza(zzflj zzfljVar) throws IOException {
        while (true) {
            int iZzcxx = zzfljVar.zzcxx();
            switch (iZzcxx) {
                case 0:
                    return this;
                case 10:
                    this.zzpzs = zzfljVar.readString();
                    break;
                case 18:
                    this.zzpzt = zzfljVar.readString();
                    break;
                case 24:
                    this.zzpzu = zzfljVar.zzcxz();
                    break;
                case 34:
                    this.zzpzv = zzfljVar.readString();
                    break;
                case 40:
                    this.zzpzw = zzfljVar.zzcxz();
                    break;
                case 48:
                    this.zzgoc = zzfljVar.zzcxz();
                    break;
                case 58:
                    this.zzpzx = zzfljVar.readString();
                    break;
                case 66:
                    this.zzpzy = zzfljVar.readString();
                    break;
                case 74:
                    this.zzpzz = zzfljVar.readString();
                    break;
                case 82:
                    this.zzqaa = zzfljVar.readString();
                    break;
                case 90:
                    this.zzqab = zzfljVar.readString();
                    break;
                case 96:
                    this.zzqac = zzfljVar.zzcya();
                    break;
                case 106:
                    int iZzb = zzflv.zzb(zzfljVar, 106);
                    int length = this.zzqad == null ? 0 : this.zzqad.length;
                    zzfmu[] zzfmuVarArr = new zzfmu[iZzb + length];
                    if (length != 0) {
                        System.arraycopy(this.zzqad, 0, zzfmuVarArr, 0, length);
                    }
                    while (length < zzfmuVarArr.length - 1) {
                        zzfmuVarArr[length] = new zzfmu();
                        zzfljVar.zza(zzfmuVarArr[length]);
                        zzfljVar.zzcxx();
                        length++;
                    }
                    zzfmuVarArr[length] = new zzfmu();
                    zzfljVar.zza(zzfmuVarArr[length]);
                    this.zzqad = zzfmuVarArr;
                    break;
                default:
                    if (!super.zza(zzfljVar, iZzcxx)) {
                        return this;
                    }
                    break;
                    break;
            }
        }
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    public final void zza(zzflk zzflkVar) throws IOException {
        if (this.zzpzs != null && !this.zzpzs.equals("")) {
            zzflkVar.zzp(1, this.zzpzs);
        }
        if (this.zzpzt != null && !this.zzpzt.equals("")) {
            zzflkVar.zzp(2, this.zzpzt);
        }
        if (this.zzpzu != 0) {
            zzflkVar.zzf(3, this.zzpzu);
        }
        if (this.zzpzv != null && !this.zzpzv.equals("")) {
            zzflkVar.zzp(4, this.zzpzv);
        }
        if (this.zzpzw != 0) {
            zzflkVar.zzf(5, this.zzpzw);
        }
        if (this.zzgoc != 0) {
            zzflkVar.zzf(6, this.zzgoc);
        }
        if (this.zzpzx != null && !this.zzpzx.equals("")) {
            zzflkVar.zzp(7, this.zzpzx);
        }
        if (this.zzpzy != null && !this.zzpzy.equals("")) {
            zzflkVar.zzp(8, this.zzpzy);
        }
        if (this.zzpzz != null && !this.zzpzz.equals("")) {
            zzflkVar.zzp(9, this.zzpzz);
        }
        if (this.zzqaa != null && !this.zzqaa.equals("")) {
            zzflkVar.zzp(10, this.zzqaa);
        }
        if (this.zzqab != null && !this.zzqab.equals("")) {
            zzflkVar.zzp(11, this.zzqab);
        }
        if (this.zzqac != 0) {
            zzflkVar.zzad(12, this.zzqac);
        }
        if (this.zzqad != null && this.zzqad.length > 0) {
            for (int i = 0; i < this.zzqad.length; i++) {
                zzfmu zzfmuVar = this.zzqad[i];
                if (zzfmuVar != null) {
                    zzflkVar.zza(13, zzfmuVar);
                }
            }
        }
        super.zza(zzflkVar);
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    protected final int zzq() {
        int iZzq = super.zzq();
        if (this.zzpzs != null && !this.zzpzs.equals("")) {
            iZzq += zzflk.zzq(1, this.zzpzs);
        }
        if (this.zzpzt != null && !this.zzpzt.equals("")) {
            iZzq += zzflk.zzq(2, this.zzpzt);
        }
        if (this.zzpzu != 0) {
            iZzq += zzflk.zzc(3, this.zzpzu);
        }
        if (this.zzpzv != null && !this.zzpzv.equals("")) {
            iZzq += zzflk.zzq(4, this.zzpzv);
        }
        if (this.zzpzw != 0) {
            iZzq += zzflk.zzc(5, this.zzpzw);
        }
        if (this.zzgoc != 0) {
            iZzq += zzflk.zzc(6, this.zzgoc);
        }
        if (this.zzpzx != null && !this.zzpzx.equals("")) {
            iZzq += zzflk.zzq(7, this.zzpzx);
        }
        if (this.zzpzy != null && !this.zzpzy.equals("")) {
            iZzq += zzflk.zzq(8, this.zzpzy);
        }
        if (this.zzpzz != null && !this.zzpzz.equals("")) {
            iZzq += zzflk.zzq(9, this.zzpzz);
        }
        if (this.zzqaa != null && !this.zzqaa.equals("")) {
            iZzq += zzflk.zzq(10, this.zzqaa);
        }
        if (this.zzqab != null && !this.zzqab.equals("")) {
            iZzq += zzflk.zzq(11, this.zzqab);
        }
        if (this.zzqac != 0) {
            iZzq += zzflk.zzag(12, this.zzqac);
        }
        if (this.zzqad != null && this.zzqad.length > 0) {
            for (int i = 0; i < this.zzqad.length; i++) {
                zzfmu zzfmuVar = this.zzqad[i];
                if (zzfmuVar != null) {
                    iZzq += zzflk.zzb(13, zzfmuVar);
                }
            }
        }
        return iZzq;
    }
}
