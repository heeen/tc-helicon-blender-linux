package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzchq implements Parcelable.Creator<zzchp> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzchp createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        double dZzn = 0.0d;
        double dZzn2 = 0.0d;
        int iZzg = 0;
        short sZzf = 0;
        int iZzg2 = 0;
        String strZzq = null;
        float fZzl = 0.0f;
        long jZzi = 0;
        int iZzg3 = -1;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 2:
                    jZzi = zzbgm.zzi(parcel, i);
                    break;
                case 3:
                    sZzf = zzbgm.zzf(parcel, i);
                    break;
                case 4:
                    dZzn = zzbgm.zzn(parcel, i);
                    break;
                case 5:
                    dZzn2 = zzbgm.zzn(parcel, i);
                    break;
                case 6:
                    fZzl = zzbgm.zzl(parcel, i);
                    break;
                case 7:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 8:
                    iZzg2 = zzbgm.zzg(parcel, i);
                    break;
                case 9:
                    iZzg3 = zzbgm.zzg(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzchp(strZzq, iZzg, sZzf, dZzn, dZzn2, fZzl, jZzi, iZzg2, iZzg3);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzchp[] newArray(int i) {
        return new zzchp[i];
    }
}
