package com.google.android.gms.phenotype;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import com.google.android.gms.common.annotation.KeepForSdk;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
@KeepForSdk
public class ExperimentTokens extends zzbgl {

    @KeepForSdk
    public static final Parcelable.Creator<ExperimentTokens> CREATOR = new zzh();
    private static byte[][] zzfpb = new byte[0][];
    private static ExperimentTokens zzkfz = new ExperimentTokens("", null, zzfpb, zzfpb, zzfpb, zzfpb, null, null);
    private static final zza zzkgi = new zzd();
    private static final zza zzkgj = new zze();
    private static final zza zzkgk = new zzf();
    private static final zza zzkgl = new zzg();
    private String zzkga;
    private byte[] zzkgb;
    private byte[][] zzkgc;
    private byte[][] zzkgd;
    private byte[][] zzkge;
    private byte[][] zzkgf;
    private int[] zzkgg;
    private byte[][] zzkgh;

    interface zza {
    }

    public ExperimentTokens(String str, byte[] bArr, byte[][] bArr2, byte[][] bArr3, byte[][] bArr4, byte[][] bArr5, int[] iArr, byte[][] bArr6) {
        this.zzkga = str;
        this.zzkgb = bArr;
        this.zzkgc = bArr2;
        this.zzkgd = bArr3;
        this.zzkge = bArr4;
        this.zzkgf = bArr5;
        this.zzkgg = iArr;
        this.zzkgh = bArr6;
    }

    private static void zza(StringBuilder sb, String str, int[] iArr) {
        String str2;
        sb.append(str);
        sb.append("=");
        if (iArr == null) {
            str2 = "null";
        } else {
            sb.append("(");
            int length = iArr.length;
            boolean z = true;
            int i = 0;
            while (i < length) {
                int i2 = iArr[i];
                if (!z) {
                    sb.append(", ");
                }
                sb.append(i2);
                i++;
                z = false;
            }
            str2 = ")";
        }
        sb.append(str2);
    }

    private static void zza(StringBuilder sb, String str, byte[][] bArr) {
        String str2;
        sb.append(str);
        sb.append("=");
        if (bArr == null) {
            str2 = "null";
        } else {
            sb.append("(");
            int length = bArr.length;
            boolean z = true;
            int i = 0;
            while (i < length) {
                byte[] bArr2 = bArr[i];
                if (!z) {
                    sb.append(", ");
                }
                sb.append("'");
                sb.append(Base64.encodeToString(bArr2, 3));
                sb.append("'");
                i++;
                z = false;
            }
            str2 = ")";
        }
        sb.append(str2);
    }

    private static List<String> zzb(byte[][] bArr) {
        if (bArr == null) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList(bArr.length);
        for (byte[] bArr2 : bArr) {
            arrayList.add(Base64.encodeToString(bArr2, 3));
        }
        Collections.sort(arrayList);
        return arrayList;
    }

    private static List<Integer> zzd(int[] iArr) {
        if (iArr == null) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList(iArr.length);
        for (int i : iArr) {
            arrayList.add(Integer.valueOf(i));
        }
        Collections.sort(arrayList);
        return arrayList;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ExperimentTokens) {
            ExperimentTokens experimentTokens = (ExperimentTokens) obj;
            if (zzn.equals(this.zzkga, experimentTokens.zzkga) && Arrays.equals(this.zzkgb, experimentTokens.zzkgb) && zzn.equals(zzb(this.zzkgc), zzb(experimentTokens.zzkgc)) && zzn.equals(zzb(this.zzkgd), zzb(experimentTokens.zzkgd)) && zzn.equals(zzb(this.zzkge), zzb(experimentTokens.zzkge)) && zzn.equals(zzb(this.zzkgf), zzb(experimentTokens.zzkgf)) && zzn.equals(zzd(this.zzkgg), zzd(experimentTokens.zzkgg)) && zzn.equals(zzb(this.zzkgh), zzb(experimentTokens.zzkgh))) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        String string;
        String str;
        StringBuilder sb = new StringBuilder("ExperimentTokens");
        sb.append("(");
        if (this.zzkga == null) {
            string = "null";
        } else {
            String str2 = this.zzkga;
            StringBuilder sb2 = new StringBuilder(String.valueOf(str2).length() + 2);
            sb2.append("'");
            sb2.append(str2);
            sb2.append("'");
            string = sb2.toString();
        }
        sb.append(string);
        sb.append(", ");
        byte[] bArr = this.zzkgb;
        sb.append("direct");
        sb.append("=");
        if (bArr == null) {
            str = "null";
        } else {
            sb.append("'");
            sb.append(Base64.encodeToString(bArr, 3));
            str = "'";
        }
        sb.append(str);
        sb.append(", ");
        zza(sb, "GAIA", this.zzkgc);
        sb.append(", ");
        zza(sb, "PSEUDO", this.zzkgd);
        sb.append(", ");
        zza(sb, "ALWAYS", this.zzkge);
        sb.append(", ");
        zza(sb, "OTHER", this.zzkgf);
        sb.append(", ");
        zza(sb, "weak", this.zzkgg);
        sb.append(", ");
        zza(sb, "directs", this.zzkgh);
        sb.append(")");
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 2, this.zzkga, false);
        zzbgo.zza(parcel, 3, this.zzkgb, false);
        zzbgo.zza(parcel, 4, this.zzkgc, false);
        zzbgo.zza(parcel, 5, this.zzkgd, false);
        zzbgo.zza(parcel, 6, this.zzkge, false);
        zzbgo.zza(parcel, 7, this.zzkgf, false);
        zzbgo.zza(parcel, 8, this.zzkgg, false);
        zzbgo.zza(parcel, 9, this.zzkgh, false);
        zzbgo.zzai(parcel, iZze);
    }
}
