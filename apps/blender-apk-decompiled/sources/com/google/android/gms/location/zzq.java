package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;
import com.google.android.gms.internal.zzchp;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzq implements Parcelable.Creator<GeofencingRequest> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ GeofencingRequest createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        String strZzq = "";
        ArrayList arrayListZzc = null;
        int iZzg = 0;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    arrayListZzc = zzbgm.zzc(parcel, i, zzchp.CREATOR);
                    break;
                case 2:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 3:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new GeofencingRequest(arrayListZzc, iZzg, strZzq);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ GeofencingRequest[] newArray(int i) {
        return new GeofencingRequest[i];
    }
}
