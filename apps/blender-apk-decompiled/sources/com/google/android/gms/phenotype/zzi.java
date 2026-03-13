package com.google.android.gms.phenotype;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.util.Arrays;
import java.util.Comparator;

/* JADX INFO: loaded from: classes.dex */
public final class zzi extends zzbgl implements Comparable<zzi> {
    public static final Parcelable.Creator<zzi> CREATOR = new zzk();
    private static Comparator<zzi> zzkgq = new zzj();
    public final String name;
    private String zzgim;
    private boolean zzili;
    private double zzilk;
    private long zzkgm;
    private byte[] zzkgn;
    private int zzkgo;
    public final int zzkgp;

    public zzi(String str, long j, boolean z, double d, String str2, byte[] bArr, int i, int i2) {
        this.name = str;
        this.zzkgm = j;
        this.zzili = z;
        this.zzilk = d;
        this.zzgim = str2;
        this.zzkgn = bArr;
        this.zzkgo = i;
        this.zzkgp = i2;
    }

    private static int compare(int i, int i2) {
        if (i < i2) {
            return -1;
        }
        return i == i2 ? 0 : 1;
    }

    @Override // java.lang.Comparable
    public final /* synthetic */ int compareTo(zzi zziVar) {
        zzi zziVar2 = zziVar;
        int iCompareTo = this.name.compareTo(zziVar2.name);
        if (iCompareTo != 0) {
            return iCompareTo;
        }
        int iCompare = compare(this.zzkgo, zziVar2.zzkgo);
        if (iCompare != 0) {
            return iCompare;
        }
        switch (this.zzkgo) {
            case 1:
                long j = this.zzkgm;
                long j2 = zziVar2.zzkgm;
                if (j < j2) {
                    return -1;
                }
                return j == j2 ? 0 : 1;
            case 2:
                boolean z = this.zzili;
                if (z == zziVar2.zzili) {
                    return 0;
                }
                return z ? 1 : -1;
            case 3:
                return Double.compare(this.zzilk, zziVar2.zzilk);
            case 4:
                String str = this.zzgim;
                String str2 = zziVar2.zzgim;
                if (str == str2) {
                    return 0;
                }
                if (str == null) {
                    return -1;
                }
                if (str2 == null) {
                    return 1;
                }
                return str.compareTo(str2);
            case 5:
                if (this.zzkgn == zziVar2.zzkgn) {
                    return 0;
                }
                if (this.zzkgn == null) {
                    return -1;
                }
                if (zziVar2.zzkgn == null) {
                    return 1;
                }
                for (int i = 0; i < Math.min(this.zzkgn.length, zziVar2.zzkgn.length); i++) {
                    int i2 = this.zzkgn[i] - zziVar2.zzkgn[i];
                    if (i2 != 0) {
                        return i2;
                    }
                }
                return compare(this.zzkgn.length, zziVar2.zzkgn.length);
            default:
                int i3 = this.zzkgo;
                StringBuilder sb = new StringBuilder(31);
                sb.append("Invalid enum value: ");
                sb.append(i3);
                throw new AssertionError(sb.toString());
        }
    }

    public final boolean equals(Object obj) {
        if (obj instanceof zzi) {
            zzi zziVar = (zzi) obj;
            if (zzn.equals(this.name, zziVar.name) && this.zzkgo == zziVar.zzkgo && this.zzkgp == zziVar.zzkgp) {
                switch (this.zzkgo) {
                    case 1:
                        if (this.zzkgm == zziVar.zzkgm) {
                            return true;
                        }
                        break;
                    case 2:
                        return this.zzili == zziVar.zzili;
                    case 3:
                        return this.zzilk == zziVar.zzilk;
                    case 4:
                        return zzn.equals(this.zzgim, zziVar.zzgim);
                    case 5:
                        return Arrays.equals(this.zzkgn, zziVar.zzkgn);
                    default:
                        int i = this.zzkgo;
                        StringBuilder sb = new StringBuilder(31);
                        sb.append("Invalid enum value: ");
                        sb.append(i);
                        throw new AssertionError(sb.toString());
                }
            }
        }
        return false;
    }

    public final String toString() {
        String strEncodeToString;
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append("Flag(");
        sb.append(this.name);
        sb.append(", ");
        switch (this.zzkgo) {
            case 1:
                sb.append(this.zzkgm);
                sb.append(", ");
                sb.append(this.zzkgo);
                sb.append(", ");
                sb.append(this.zzkgp);
                sb.append(")");
                return sb.toString();
            case 2:
                sb.append(this.zzili);
                sb.append(", ");
                sb.append(this.zzkgo);
                sb.append(", ");
                sb.append(this.zzkgp);
                sb.append(")");
                return sb.toString();
            case 3:
                sb.append(this.zzilk);
                sb.append(", ");
                sb.append(this.zzkgo);
                sb.append(", ");
                sb.append(this.zzkgp);
                sb.append(")");
                return sb.toString();
            case 4:
                sb.append("'");
                strEncodeToString = this.zzgim;
                sb.append(strEncodeToString);
                str = "'";
                sb.append(str);
                sb.append(", ");
                sb.append(this.zzkgo);
                sb.append(", ");
                sb.append(this.zzkgp);
                sb.append(")");
                return sb.toString();
            case 5:
                if (this.zzkgn == null) {
                    str = "null";
                    sb.append(str);
                    sb.append(", ");
                    sb.append(this.zzkgo);
                    sb.append(", ");
                    sb.append(this.zzkgp);
                    sb.append(")");
                    return sb.toString();
                }
                sb.append("'");
                strEncodeToString = Base64.encodeToString(this.zzkgn, 3);
                sb.append(strEncodeToString);
                str = "'";
                sb.append(str);
                sb.append(", ");
                sb.append(this.zzkgo);
                sb.append(", ");
                sb.append(this.zzkgp);
                sb.append(")");
                return sb.toString();
            default:
                String str2 = this.name;
                int i = this.zzkgo;
                StringBuilder sb2 = new StringBuilder(String.valueOf(str2).length() + 27);
                sb2.append("Invalid type: ");
                sb2.append(str2);
                sb2.append(", ");
                sb2.append(i);
                throw new AssertionError(sb2.toString());
        }
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 2, this.name, false);
        zzbgo.zza(parcel, 3, this.zzkgm);
        zzbgo.zza(parcel, 4, this.zzili);
        zzbgo.zza(parcel, 5, this.zzilk);
        zzbgo.zza(parcel, 6, this.zzgim, false);
        zzbgo.zza(parcel, 7, this.zzkgn, false);
        zzbgo.zzc(parcel, 8, this.zzkgo);
        zzbgo.zzc(parcel, 9, this.zzkgp);
        zzbgo.zzai(parcel, iZze);
    }
}
