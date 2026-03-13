package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.location.LocationRequest;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzchm implements Parcelable.Creator<zzchl> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzchl createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        List<zzcfs> listZzc = zzchl.zzitm;
        boolean zZzc = false;
        boolean zZzc2 = false;
        boolean zZzc3 = false;
        LocationRequest locationRequest = null;
        String strZzq = null;
        String strZzq2 = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            int i2 = 65535 & i;
            if (i2 != 1) {
                switch (i2) {
                    case 5:
                        listZzc = zzbgm.zzc(parcel, i, zzcfs.CREATOR);
                        break;
                    case 6:
                        strZzq = zzbgm.zzq(parcel, i);
                        break;
                    case 7:
                        zZzc = zzbgm.zzc(parcel, i);
                        break;
                    case 8:
                        zZzc2 = zzbgm.zzc(parcel, i);
                        break;
                    case 9:
                        zZzc3 = zzbgm.zzc(parcel, i);
                        break;
                    case 10:
                        strZzq2 = zzbgm.zzq(parcel, i);
                        break;
                    default:
                        zzbgm.zzb(parcel, i);
                        break;
                }
            } else {
                locationRequest = (LocationRequest) zzbgm.zza(parcel, i, LocationRequest.CREATOR);
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzchl(locationRequest, listZzc, strZzq, zZzc, zZzc2, zZzc3, strZzq2);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzchl[] newArray(int i) {
        return new zzchl[i];
    }
}
