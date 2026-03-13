package com.google.android.gms.internal;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import com.google.android.gms.common.internal.zzbq;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public class zzbia extends zzbhs {
    public static final Parcelable.Creator<zzbia> CREATOR = new zzbib();
    private final String mClassName;
    private final int zzehz;
    private final zzbhv zzgiw;
    private final Parcel zzgjd;
    private final int zzgje = 2;
    private int zzgjf;
    private int zzgjg;

    zzbia(int i, Parcel parcel, zzbhv zzbhvVar) {
        this.zzehz = i;
        this.zzgjd = (Parcel) zzbq.checkNotNull(parcel);
        this.zzgiw = zzbhvVar;
        this.mClassName = this.zzgiw == null ? null : this.zzgiw.zzanj();
        this.zzgjf = 2;
    }

    private static void zza(StringBuilder sb, int i, Object obj) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                sb.append(obj);
                return;
            case 7:
                sb.append("\"");
                sb.append(com.google.android.gms.common.util.zzq.zzha(obj.toString()));
                sb.append("\"");
                return;
            case 8:
                sb.append("\"");
                sb.append(com.google.android.gms.common.util.zzc.zzj((byte[]) obj));
                sb.append("\"");
                return;
            case 9:
                sb.append("\"");
                sb.append(com.google.android.gms.common.util.zzc.zzk((byte[]) obj));
                sb.append("\"");
                return;
            case 10:
                com.google.android.gms.common.util.zzr.zza(sb, (HashMap) obj);
                return;
            case 11:
                throw new IllegalArgumentException("Method does not accept concrete type.");
            default:
                StringBuilder sb2 = new StringBuilder(26);
                sb2.append("Unknown type = ");
                sb2.append(i);
                throw new IllegalArgumentException(sb2.toString());
        }
    }

    private final void zza(StringBuilder sb, zzbhq<?, ?> zzbhqVar, Parcel parcel, int i) {
        int i2 = 0;
        if (zzbhqVar.zzgir) {
            sb.append("[");
            BigInteger[] bigIntegerArr = null;
            double[] dArrCreateDoubleArray = null;
            switch (zzbhqVar.zzgiq) {
                case 0:
                    int[] iArrZzw = zzbgm.zzw(parcel, i);
                    int length = iArrZzw.length;
                    while (i2 < length) {
                        if (i2 != 0) {
                            sb.append(",");
                        }
                        sb.append(Integer.toString(iArrZzw[i2]));
                        i2++;
                    }
                    break;
                case 1:
                    int iZza = zzbgm.zza(parcel, i);
                    int iDataPosition = parcel.dataPosition();
                    if (iZza != 0) {
                        int i3 = parcel.readInt();
                        bigIntegerArr = new BigInteger[i3];
                        while (i2 < i3) {
                            bigIntegerArr[i2] = new BigInteger(parcel.createByteArray());
                            i2++;
                        }
                        parcel.setDataPosition(iDataPosition + iZza);
                    }
                    com.google.android.gms.common.util.zzb.zza(sb, bigIntegerArr);
                    break;
                case 2:
                    com.google.android.gms.common.util.zzb.zza(sb, zzbgm.zzx(parcel, i));
                    break;
                case 3:
                    com.google.android.gms.common.util.zzb.zza(sb, zzbgm.zzy(parcel, i));
                    break;
                case 4:
                    int iZza2 = zzbgm.zza(parcel, i);
                    int iDataPosition2 = parcel.dataPosition();
                    if (iZza2 != 0) {
                        dArrCreateDoubleArray = parcel.createDoubleArray();
                        parcel.setDataPosition(iDataPosition2 + iZza2);
                    }
                    com.google.android.gms.common.util.zzb.zza(sb, dArrCreateDoubleArray);
                    break;
                case 5:
                    com.google.android.gms.common.util.zzb.zza(sb, zzbgm.zzz(parcel, i));
                    break;
                case 6:
                    com.google.android.gms.common.util.zzb.zza(sb, zzbgm.zzv(parcel, i));
                    break;
                case 7:
                    com.google.android.gms.common.util.zzb.zza(sb, zzbgm.zzaa(parcel, i));
                    break;
                case 8:
                case 9:
                case 10:
                    throw new UnsupportedOperationException("List of type BASE64, BASE64_URL_SAFE, or STRING_MAP is not supported");
                case 11:
                    Parcel[] parcelArrZzae = zzbgm.zzae(parcel, i);
                    int length2 = parcelArrZzae.length;
                    for (int i4 = 0; i4 < length2; i4++) {
                        if (i4 > 0) {
                            sb.append(",");
                        }
                        parcelArrZzae[i4].setDataPosition(0);
                        zza(sb, zzbhqVar.zzanh(), parcelArrZzae[i4]);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown field type out.");
            }
            sb.append("]");
            return;
        }
        switch (zzbhqVar.zzgiq) {
            case 0:
                sb.append(zzbgm.zzg(parcel, i));
                return;
            case 1:
                sb.append(zzbgm.zzk(parcel, i));
                return;
            case 2:
                sb.append(zzbgm.zzi(parcel, i));
                return;
            case 3:
                sb.append(zzbgm.zzl(parcel, i));
                return;
            case 4:
                sb.append(zzbgm.zzn(parcel, i));
                return;
            case 5:
                sb.append(zzbgm.zzp(parcel, i));
                return;
            case 6:
                sb.append(zzbgm.zzc(parcel, i));
                return;
            case 7:
                String strZzq = zzbgm.zzq(parcel, i);
                sb.append("\"");
                sb.append(com.google.android.gms.common.util.zzq.zzha(strZzq));
                sb.append("\"");
                return;
            case 8:
                byte[] bArrZzt = zzbgm.zzt(parcel, i);
                sb.append("\"");
                sb.append(com.google.android.gms.common.util.zzc.zzj(bArrZzt));
                sb.append("\"");
                return;
            case 9:
                byte[] bArrZzt2 = zzbgm.zzt(parcel, i);
                sb.append("\"");
                sb.append(com.google.android.gms.common.util.zzc.zzk(bArrZzt2));
                sb.append("\"");
                return;
            case 10:
                Bundle bundleZzs = zzbgm.zzs(parcel, i);
                Set<String> setKeySet = bundleZzs.keySet();
                setKeySet.size();
                sb.append("{");
                boolean z = true;
                for (String str : setKeySet) {
                    if (!z) {
                        sb.append(",");
                    }
                    sb.append("\"");
                    sb.append(str);
                    sb.append("\"");
                    sb.append(":");
                    sb.append("\"");
                    sb.append(com.google.android.gms.common.util.zzq.zzha(bundleZzs.getString(str)));
                    sb.append("\"");
                    z = false;
                }
                sb.append("}");
                return;
            case 11:
                Parcel parcelZzad = zzbgm.zzad(parcel, i);
                parcelZzad.setDataPosition(0);
                zza(sb, zzbhqVar.zzanh(), parcelZzad);
                return;
            default:
                throw new IllegalStateException("Unknown field type out");
        }
    }

    private final void zza(StringBuilder sb, Map<String, zzbhq<?, ?>> map, Parcel parcel) {
        Object objValueOf;
        SparseArray sparseArray = new SparseArray();
        for (Map.Entry<String, zzbhq<?, ?>> entry : map.entrySet()) {
            sparseArray.put(entry.getValue().zzgit, entry);
        }
        sb.append('{');
        int iZzd = zzbgm.zzd(parcel);
        boolean z = false;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            Map.Entry entry2 = (Map.Entry) sparseArray.get(65535 & i);
            if (entry2 != null) {
                if (z) {
                    sb.append(",");
                }
                String str = (String) entry2.getKey();
                zzbhq<?, ?> zzbhqVar = (zzbhq) entry2.getValue();
                sb.append("\"");
                sb.append(str);
                sb.append("\":");
                if (zzbhqVar.zzang()) {
                    switch (zzbhqVar.zzgiq) {
                        case 0:
                            objValueOf = Integer.valueOf(zzbgm.zzg(parcel, i));
                            break;
                        case 1:
                            objValueOf = zzbgm.zzk(parcel, i);
                            break;
                        case 2:
                            objValueOf = Long.valueOf(zzbgm.zzi(parcel, i));
                            break;
                        case 3:
                            objValueOf = Float.valueOf(zzbgm.zzl(parcel, i));
                            break;
                        case 4:
                            objValueOf = Double.valueOf(zzbgm.zzn(parcel, i));
                            break;
                        case 5:
                            objValueOf = zzbgm.zzp(parcel, i);
                            break;
                        case 6:
                            objValueOf = Boolean.valueOf(zzbgm.zzc(parcel, i));
                            break;
                        case 7:
                            objValueOf = zzbgm.zzq(parcel, i);
                            break;
                        case 8:
                        case 9:
                            objValueOf = zzbgm.zzt(parcel, i);
                            break;
                        case 10:
                            objValueOf = zzm(zzbgm.zzs(parcel, i));
                            break;
                        case 11:
                            throw new IllegalArgumentException("Method does not accept concrete type.");
                        default:
                            int i2 = zzbhqVar.zzgiq;
                            StringBuilder sb2 = new StringBuilder(36);
                            sb2.append("Unknown field out type = ");
                            sb2.append(i2);
                            throw new IllegalArgumentException(sb2.toString());
                    }
                    zzb(sb, zzbhqVar, zza(zzbhqVar, objValueOf));
                } else {
                    zza(sb, zzbhqVar, parcel, i);
                }
                z = true;
            }
        }
        if (parcel.dataPosition() == iZzd) {
            sb.append('}');
            return;
        }
        StringBuilder sb3 = new StringBuilder(37);
        sb3.append("Overread allowed size end=");
        sb3.append(iZzd);
        throw new zzbgn(sb3.toString(), parcel);
    }

    private Parcel zzanl() {
        switch (this.zzgjf) {
            case 0:
                this.zzgjg = zzbgo.zze(this.zzgjd);
            case 1:
                zzbgo.zzai(this.zzgjd, this.zzgjg);
                this.zzgjf = 2;
                break;
        }
        return this.zzgjd;
    }

    private final void zzb(StringBuilder sb, zzbhq<?, ?> zzbhqVar, Object obj) {
        if (!zzbhqVar.zzgip) {
            zza(sb, zzbhqVar.zzgio, obj);
            return;
        }
        ArrayList arrayList = (ArrayList) obj;
        sb.append("[");
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                sb.append(",");
            }
            zza(sb, zzbhqVar.zzgio, arrayList.get(i));
        }
        sb.append("]");
    }

    private static HashMap<String, String> zzm(Bundle bundle) {
        HashMap<String, String> map = new HashMap<>();
        for (String str : bundle.keySet()) {
            map.put(str, bundle.getString(str));
        }
        return map;
    }

    @Override // com.google.android.gms.internal.zzbhp
    public String toString() {
        zzbq.checkNotNull(this.zzgiw, "Cannot convert to JSON on client side.");
        Parcel parcelZzanl = zzanl();
        parcelZzanl.setDataPosition(0);
        StringBuilder sb = new StringBuilder(100);
        zza(sb, this.zzgiw.zzgz(this.mClassName), parcelZzanl);
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        zzbhv zzbhvVar;
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        zzbgo.zza(parcel, 2, zzanl(), false);
        switch (this.zzgje) {
            case 0:
                zzbhvVar = null;
                break;
            case 1:
            case 2:
                zzbhvVar = this.zzgiw;
                break;
            default:
                int i2 = this.zzgje;
                StringBuilder sb = new StringBuilder(34);
                sb.append("Invalid creation type: ");
                sb.append(i2);
                throw new IllegalStateException(sb.toString());
        }
        zzbgo.zza(parcel, 3, (Parcelable) zzbhvVar, i, false);
        zzbgo.zzai(parcel, iZze);
    }

    @Override // com.google.android.gms.internal.zzbhp
    public final Map<String, zzbhq<?, ?>> zzabz() {
        if (this.zzgiw == null) {
            return null;
        }
        return this.zzgiw.zzgz(this.mClassName);
    }

    @Override // com.google.android.gms.internal.zzbhs, com.google.android.gms.internal.zzbhp
    public final Object zzgx(String str) {
        throw new UnsupportedOperationException("Converting to JSON does not require this method.");
    }

    @Override // com.google.android.gms.internal.zzbhs, com.google.android.gms.internal.zzbhp
    public final boolean zzgy(String str) {
        throw new UnsupportedOperationException("Converting to JSON does not require this method.");
    }
}
