package com.google.android.gms.location;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzac implements Parcelable.Creator<LocationResult> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ LocationResult createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        List<Location> listZzc = LocationResult.zzisl;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            if ((65535 & i) != 1) {
                zzbgm.zzb(parcel, i);
            } else {
                listZzc = zzbgm.zzc(parcel, i, Location.CREATOR);
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new LocationResult(listZzc);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ LocationResult[] newArray(int i) {
        return new LocationResult[i];
    }
}
