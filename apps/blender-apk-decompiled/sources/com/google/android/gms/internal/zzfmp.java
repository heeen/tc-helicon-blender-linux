package com.google.android.gms.internal;

import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class zzfmp extends zzflm<zzfmp> implements Cloneable {
    private int zzjgl = 0;
    private String zzpyp = "";
    private String version = "";

    public zzfmp() {
        this.zzpvl = null;
        this.zzpnr = -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzddc, reason: merged with bridge method [inline-methods] */
    public zzfmp clone() {
        try {
            return (zzfmp) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof zzfmp)) {
            return false;
        }
        zzfmp zzfmpVar = (zzfmp) obj;
        if (this.zzjgl != zzfmpVar.zzjgl) {
            return false;
        }
        if (this.zzpyp == null) {
            if (zzfmpVar.zzpyp != null) {
                return false;
            }
        } else if (!this.zzpyp.equals(zzfmpVar.zzpyp)) {
            return false;
        }
        if (this.version == null) {
            if (zzfmpVar.version != null) {
                return false;
            }
        } else if (!this.version.equals(zzfmpVar.version)) {
            return false;
        }
        return (this.zzpvl == null || this.zzpvl.isEmpty()) ? zzfmpVar.zzpvl == null || zzfmpVar.zzpvl.isEmpty() : this.zzpvl.equals(zzfmpVar.zzpvl);
    }

    public final int hashCode() {
        int iHashCode = 0;
        int iHashCode2 = (((((((getClass().getName().hashCode() + 527) * 31) + this.zzjgl) * 31) + (this.zzpyp == null ? 0 : this.zzpyp.hashCode())) * 31) + (this.version == null ? 0 : this.version.hashCode())) * 31;
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
            if (iZzcxx == 8) {
                this.zzjgl = zzfljVar.zzcya();
            } else if (iZzcxx == 18) {
                this.zzpyp = zzfljVar.readString();
            } else if (iZzcxx == 26) {
                this.version = zzfljVar.readString();
            } else if (!super.zza(zzfljVar, iZzcxx)) {
                return this;
            }
        }
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    public final void zza(zzflk zzflkVar) throws IOException {
        if (this.zzjgl != 0) {
            zzflkVar.zzad(1, this.zzjgl);
        }
        if (this.zzpyp != null && !this.zzpyp.equals("")) {
            zzflkVar.zzp(2, this.zzpyp);
        }
        if (this.version != null && !this.version.equals("")) {
            zzflkVar.zzp(3, this.version);
        }
        super.zza(zzflkVar);
    }

    @Override // com.google.android.gms.internal.zzflm
    /* JADX INFO: renamed from: zzdck */
    public final /* synthetic */ zzflm clone() throws CloneNotSupportedException {
        return (zzfmp) clone();
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    /* JADX INFO: renamed from: zzdcl */
    public final /* synthetic */ zzfls clone() throws CloneNotSupportedException {
        return (zzfmp) clone();
    }

    @Override // com.google.android.gms.internal.zzflm, com.google.android.gms.internal.zzfls
    protected final int zzq() {
        int iZzq = super.zzq();
        if (this.zzjgl != 0) {
            iZzq += zzflk.zzag(1, this.zzjgl);
        }
        if (this.zzpyp != null && !this.zzpyp.equals("")) {
            iZzq += zzflk.zzq(2, this.zzpyp);
        }
        return (this.version == null || this.version.equals("")) ? iZzq : iZzq + zzflk.zzq(3, this.version);
    }
}
