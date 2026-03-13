package com.google.android.gms.phenotype;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzh implements Parcelable.Creator<ExperimentTokens> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ ExperimentTokens createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        String strZzq = null;
        byte[] bArrZzt = null;
        byte[][] bArrZzu = null;
        byte[][] bArrZzu2 = null;
        byte[][] bArrZzu3 = null;
        byte[][] bArrZzu4 = null;
        int[] iArrZzw = null;
        byte[][] bArrZzu5 = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 2:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 3:
                    bArrZzt = zzbgm.zzt(parcel, i);
                    break;
                case 4:
                    bArrZzu = zzbgm.zzu(parcel, i);
                    break;
                case 5:
                    bArrZzu2 = zzbgm.zzu(parcel, i);
                    break;
                case 6:
                    bArrZzu3 = zzbgm.zzu(parcel, i);
                    break;
                case 7:
                    bArrZzu4 = zzbgm.zzu(parcel, i);
                    break;
                case 8:
                    iArrZzw = zzbgm.zzw(parcel, i);
                    break;
                case 9:
                    bArrZzu5 = zzbgm.zzu(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new ExperimentTokens(strZzq, bArrZzt, bArrZzu, bArrZzu2, bArrZzu3, bArrZzu4, iArrZzw, bArrZzu5);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ ExperimentTokens[] newArray(int i) {
        return new ExperimentTokens[i];
    }
}
