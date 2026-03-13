package com.google.android.gms.clearcut;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbfv;
import com.google.android.gms.internal.zzbgm;
import com.google.android.gms.phenotype.ExperimentTokens;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzf implements Parcelable.Creator<zze> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zze createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        zzbfv zzbfvVar = null;
        byte[] bArrZzt = null;
        int[] iArrZzw = null;
        String[] strArrZzaa = null;
        int[] iArrZzw2 = null;
        byte[][] bArrZzu = null;
        ExperimentTokens[] experimentTokensArr = null;
        boolean zZzc = true;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 2:
                    zzbfvVar = (zzbfv) zzbgm.zza(parcel, i, zzbfv.CREATOR);
                    break;
                case 3:
                    bArrZzt = zzbgm.zzt(parcel, i);
                    break;
                case 4:
                    iArrZzw = zzbgm.zzw(parcel, i);
                    break;
                case 5:
                    strArrZzaa = zzbgm.zzaa(parcel, i);
                    break;
                case 6:
                    iArrZzw2 = zzbgm.zzw(parcel, i);
                    break;
                case 7:
                    bArrZzu = zzbgm.zzu(parcel, i);
                    break;
                case 8:
                    zZzc = zzbgm.zzc(parcel, i);
                    break;
                case 9:
                    experimentTokensArr = (ExperimentTokens[]) zzbgm.zzb(parcel, i, ExperimentTokens.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zze(zzbfvVar, bArrZzt, iArrZzw, strArrZzaa, iArrZzw2, bArrZzu, zZzc, experimentTokensArr);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zze[] newArray(int i) {
        return new zze[i];
    }
}
