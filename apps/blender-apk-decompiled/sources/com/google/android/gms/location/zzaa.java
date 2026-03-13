package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzaa implements Parcelable.Creator<LocationAvailability> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ LocationAvailability createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 1;
        int iZzg2 = 1;
        int iZzg3 = 1000;
        long jZzi = 0;
        zzaj[] zzajVarArr = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    iZzg2 = zzbgm.zzg(parcel, i);
                    break;
                case 3:
                    jZzi = zzbgm.zzi(parcel, i);
                    break;
                case 4:
                    iZzg3 = zzbgm.zzg(parcel, i);
                    break;
                case 5:
                    zzajVarArr = (zzaj[]) zzbgm.zzb(parcel, i, zzaj.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new LocationAvailability(iZzg3, iZzg, iZzg2, jZzi, zzajVarArr);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ LocationAvailability[] newArray(int i) {
        return new LocationAvailability[i];
    }
}
