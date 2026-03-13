package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzai implements Parcelable.Creator<LocationSettingsStates> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ LocationSettingsStates createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        boolean zZzc = false;
        boolean zZzc2 = false;
        boolean zZzc3 = false;
        boolean zZzc4 = false;
        boolean zZzc5 = false;
        boolean zZzc6 = false;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    zZzc = zzbgm.zzc(parcel, i);
                    break;
                case 2:
                    zZzc2 = zzbgm.zzc(parcel, i);
                    break;
                case 3:
                    zZzc3 = zzbgm.zzc(parcel, i);
                    break;
                case 4:
                    zZzc4 = zzbgm.zzc(parcel, i);
                    break;
                case 5:
                    zZzc5 = zzbgm.zzc(parcel, i);
                    break;
                case 6:
                    zZzc6 = zzbgm.zzc(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new LocationSettingsStates(zZzc, zZzc2, zZzc3, zZzc4, zZzc5, zZzc6);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ LocationSettingsStates[] newArray(int i) {
        return new LocationSettingsStates[i];
    }
}
