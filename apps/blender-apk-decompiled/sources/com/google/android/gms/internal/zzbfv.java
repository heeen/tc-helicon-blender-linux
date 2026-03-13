package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbg;
import com.google.android.gms.common.internal.zzbq;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbfv extends zzbgl {
    public static final Parcelable.Creator<zzbfv> CREATOR = new zzbfw();
    private String packageName;
    private int zzfpc;
    public final String zzfpd;
    public final int zzfpe;
    private String zzfpf;
    private String zzfpg;
    private boolean zzfph;
    private int zzfpi;
    private boolean zzfqq;

    public zzbfv(String str, int i, int i2, String str2, String str3, String str4, boolean z, int i3) {
        this.packageName = (String) zzbq.checkNotNull(str);
        this.zzfpc = i;
        this.zzfpe = i2;
        this.zzfpd = str2;
        this.zzfpf = str3;
        this.zzfpg = str4;
        this.zzfqq = !z;
        this.zzfph = z;
        this.zzfpi = i3;
    }

    public zzbfv(String str, int i, int i2, String str2, String str3, boolean z, String str4, boolean z2, int i3) {
        this.packageName = str;
        this.zzfpc = i;
        this.zzfpe = i2;
        this.zzfpf = str2;
        this.zzfpg = str3;
        this.zzfqq = z;
        this.zzfpd = str4;
        this.zzfph = z2;
        this.zzfpi = i3;
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof zzbfv) {
            zzbfv zzbfvVar = (zzbfv) obj;
            if (zzbg.equal(this.packageName, zzbfvVar.packageName) && this.zzfpc == zzbfvVar.zzfpc && this.zzfpe == zzbfvVar.zzfpe && zzbg.equal(this.zzfpd, zzbfvVar.zzfpd) && zzbg.equal(this.zzfpf, zzbfvVar.zzfpf) && zzbg.equal(this.zzfpg, zzbfvVar.zzfpg) && this.zzfqq == zzbfvVar.zzfqq && this.zzfph == zzbfvVar.zzfph && this.zzfpi == zzbfvVar.zzfpi) {
                return true;
            }
        }
        return false;
    }

    public final int hashCode() {
        return Arrays.hashCode(new Object[]{this.packageName, Integer.valueOf(this.zzfpc), Integer.valueOf(this.zzfpe), this.zzfpd, this.zzfpf, this.zzfpg, Boolean.valueOf(this.zzfqq), Boolean.valueOf(this.zzfph), Integer.valueOf(this.zzfpi)});
    }

    public final String toString() {
        return "PlayLoggerContext[package=" + this.packageName + ",packageVersionCode=" + this.zzfpc + ",logSource=" + this.zzfpe + ",logSourceName=" + this.zzfpd + ",uploadAccount=" + this.zzfpf + ",loggingId=" + this.zzfpg + ",logAndroidId=" + this.zzfqq + ",isAnonymous=" + this.zzfph + ",qosTier=" + this.zzfpi + "]";
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 2, this.packageName, false);
        zzbgo.zzc(parcel, 3, this.zzfpc);
        zzbgo.zzc(parcel, 4, this.zzfpe);
        zzbgo.zza(parcel, 5, this.zzfpf, false);
        zzbgo.zza(parcel, 6, this.zzfpg, false);
        zzbgo.zza(parcel, 7, this.zzfqq);
        zzbgo.zza(parcel, 8, this.zzfpd, false);
        zzbgo.zza(parcel, 9, this.zzfph);
        zzbgo.zzc(parcel, 10, this.zzfpi);
        zzbgo.zzai(parcel, iZze);
    }
}
