package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzag implements Parcelable.Creator<LocationSettingsRequest> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ LocationSettingsRequest createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        boolean zZzc = false;
        ArrayList arrayListZzc = null;
        boolean zZzc2 = false;
        zzae zzaeVar = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            int i2 = 65535 & i;
            if (i2 != 5) {
                switch (i2) {
                    case 1:
                        arrayListZzc = zzbgm.zzc(parcel, i, LocationRequest.CREATOR);
                        break;
                    case 2:
                        zZzc = zzbgm.zzc(parcel, i);
                        break;
                    case 3:
                        zZzc2 = zzbgm.zzc(parcel, i);
                        break;
                    default:
                        zzbgm.zzb(parcel, i);
                        break;
                }
            } else {
                zzaeVar = (zzae) zzbgm.zza(parcel, i, zzae.CREATOR);
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new LocationSettingsRequest(arrayListZzc, zZzc, zZzc2, zzaeVar);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ LocationSettingsRequest[] newArray(int i) {
        return new LocationSettingsRequest[i];
    }
}
