package com.google.android.gms.phenotype;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzk implements Parcelable.Creator<zzi> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzi createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        boolean zZzc = false;
        int iZzg = 0;
        int iZzg2 = 0;
        String strZzq = null;
        String strZzq2 = null;
        byte[] bArrZzt = null;
        long jZzi = 0;
        double dZzn = 0.0d;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 2:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 3:
                    jZzi = zzbgm.zzi(parcel, i);
                    break;
                case 4:
                    zZzc = zzbgm.zzc(parcel, i);
                    break;
                case 5:
                    dZzn = zzbgm.zzn(parcel, i);
                    break;
                case 6:
                    strZzq2 = zzbgm.zzq(parcel, i);
                    break;
                case 7:
                    bArrZzt = zzbgm.zzt(parcel, i);
                    break;
                case 8:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 9:
                    iZzg2 = zzbgm.zzg(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzi(strZzq, jZzi, zZzc, dZzn, strZzq2, bArrZzt, iZzg, iZzg2);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzi[] newArray(int i) {
        return new zzi[i];
    }
}
