package com.google.android.gms.location.places;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzl implements Parcelable.Creator<PlaceReport> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ PlaceReport createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        String strZzq = null;
        int iZzg = 0;
        String strZzq2 = null;
        String strZzq3 = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 3:
                    strZzq2 = zzbgm.zzq(parcel, i);
                    break;
                case 4:
                    strZzq3 = zzbgm.zzq(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new PlaceReport(iZzg, strZzq, strZzq2, strZzq3);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ PlaceReport[] newArray(int i) {
        return new PlaceReport[i];
    }
}
