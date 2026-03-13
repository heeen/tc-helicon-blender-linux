package com.google.android.gms.internal;

import java.io.IOException;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class zzfmq extends zzflm<zzfmq> implements Cloneable {
    private byte[] zzpyq = zzflv.zzpwe;
    private String zzpyr = "";
    private byte[][] zzpys = zzflv.zzpwd;
    private boolean zzpyt = false;

    public zzfmq() {
        this.zzpvl = null;
        this.zzpnr = -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzddd, reason: merged with bridge method [inline-methods] */
    public zzfmq clone() {
        try {
            zzfmq zzfmqVar = (zzfmq) super.clone();
            if (this.zzpys != null && this.zzpys.length > 0) {
                zzfmqVar.zzpys = (byte[][]) this.zzpys.clone();
            }
            return zzfmqVar;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof zzfmq)) {
            return false;
        }
        zzfmq zzfmqVar = (zzfmq) obj;
        if (!Arrays.equals(this.zzpyq, zzfmqVar.zzpyq)) {
            return false;
        }
        if (this.zzpyr == null) {
            if (zzfmqVar.zzpyr != null) {
                return false;
            }
        } else if (!this.zzpyr.equals(zzfmqVar.zzpyr)) {
            return false;
        }
        if (zzflq.zza(this.zzpys, zzfmqVar.zzpys) && this.zzpyt == zzfmqVar.zzpyt) {
            return (this.zzpvl == null || this.zzpvl.isEmpty()) ? zzfmqVar.zzpvl == null || zzfmqVar.zzpvl.isEmpty() : this.zzpvl.equals(zzfmqVar.zzpvl);
        }
        return false;
    }

    public final int hashCode() {
        int iHashCode = 0;
        int iHashCode2 = (((((((((getClass().getName().hashCode() + 527) * 31) + Arrays.hashCode(this.zzpyq)) * 31) + (this.zzpyr == null ? 0 : this.zzpyr.hashCode())) * 31) + zzflq.zzd(this.zzpys)) * 31) + (this.zzpyt ? 1231 : 1237)) * 31;
        if (this.zzpvl != null && !this.zzpvl.isEmpty()) {
            iHashCode = this.zzpvl.hashCode();
        }
        return iHashCode2 + iHashCode;
    }

    @Override // com.google.android.gms.internal.zzfls
    public final /* synthetic */ zzfls zza(zzflj zzfljVar) throws IOException {
        while (true) {
            int iZzcxx = zzfljVar.zzcxx();
            if (iZzcxx == 0) {
                return this;
            }
            if (iZzcxx == 10) {
                this.zzpyq = zzfljVar.readBytes();
            } else if (iZzcxx == 18) {
                int iZzb = zzflv.zzb(zzfljVar, 18);
                int length = this.zzpys == null ? 0 : this.zzpys.length;
                byte[][] bArr = new byte[iZzb + length][];
                if (length != 0) {
                    System.arraycopy(this.zzpys, 0, bArr, 0, length);
                }
                while (length < bArr.length - 1) {
                    bArr[length] = zzfljVar.readBytes();
                    zzfljVar.zzcxx();
                    length++;
                }
                bArr[length] = zzfljVar.readBytes();
                this.zzpys = bArr;
            } else if (iZzcxx == 24) {
                this.zzpyt = zzfljVar.zzcyd();
            } else if (iZzcxx == 34) {
                this.zzpyr = zzfljVar.readString();
            } else if (!super.zza(zzfljVar, iZzcxx)) {
                return this;
            }
        }
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    public final void zza(zzflk zzflkVar) throws IOException {
        if (!Arrays.equals(this.zzpyq, zzflv.zzpwe)) {
            zzflkVar.zzc(1, this.zzpyq);
        }
        if (this.zzpys != null && this.zzpys.length > 0) {
            for (int i = 0; i < this.zzpys.length; i++) {
                byte[] bArr = this.zzpys[i];
                if (bArr != null) {
                    zzflkVar.zzc(2, bArr);
                }
            }
        }
        if (this.zzpyt) {
            zzflkVar.zzl(3, this.zzpyt);
        }
        if (this.zzpyr != null && !this.zzpyr.equals("")) {
            zzflkVar.zzp(4, this.zzpyr);
        }
        super.zza(zzflkVar);
    }

    @Override // com.google.android.gms.internal.zzflm
    /* JADX INFO: renamed from: zzdck */
    public final /* synthetic */ zzflm clone() throws CloneNotSupportedException {
        return (zzfmq) clone();
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzdcl */
    public final /* synthetic */ zzfls clone() throws CloneNotSupportedException {
        return (zzfmq) clone();
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    protected final int zzq() {
        int iZzq = super.zzq();
        if (!Arrays.equals(this.zzpyq, zzflv.zzpwe)) {
            iZzq += zzflk.zzd(1, this.zzpyq);
        }
        if (this.zzpys != null && this.zzpys.length > 0) {
            int iZzbg = 0;
            int i = 0;
            for (int i2 = 0; i2 < this.zzpys.length; i2++) {
                byte[] bArr = this.zzpys[i2];
                if (bArr != null) {
                    i++;
                    iZzbg += zzflk.zzbg(bArr);
                }
            }
            iZzq = iZzq + iZzbg + (i * 1);
        }
        if (this.zzpyt) {
            iZzq += zzflk.zzlw(3) + 1;
        }
        return (this.zzpyr == null || this.zzpyr.equals("")) ? iZzq : iZzq + zzflk.zzq(4, this.zzpyr);
    }
}
