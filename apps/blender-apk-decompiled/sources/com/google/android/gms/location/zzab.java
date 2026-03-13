package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzab implements Parcelable.Creator<LocationRequest> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ LocationRequest createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 102;
        long jZzi = 3600000;
        long jZzi2 = 600000;
        boolean zZzc = false;
        long jZzi3 = Long.MAX_VALUE;
        int iZzg2 = Integer.MAX_VALUE;
        float fZzl = 0.0f;
        long jZzi4 = 0;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    jZzi = zzbgm.zzi(parcel, i);
                    break;
                case 3:
                    jZzi2 = zzbgm.zzi(parcel, i);
                    break;
                case 4:
                    zZzc = zzbgm.zzc(parcel, i);
                    break;
                case 5:
                    jZzi3 = zzbgm.zzi(parcel, i);
                    break;
                case 6:
                    iZzg2 = zzbgm.zzg(parcel, i);
                    break;
                case 7:
                    fZzl = zzbgm.zzl(parcel, i);
                    break;
                case 8:
                    jZzi4 = zzbgm.zzi(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new LocationRequest(iZzg, jZzi, jZzi2, zZzc, jZzi3, iZzg2, fZzl, jZzi4);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ LocationRequest[] newArray(int i) {
        return new LocationRequest[i];
    }
}
