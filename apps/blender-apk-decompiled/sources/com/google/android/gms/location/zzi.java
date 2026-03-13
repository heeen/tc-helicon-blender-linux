package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzi implements Parcelable.Creator<DetectedActivity> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ DetectedActivity createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        int iZzg2 = 0;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    iZzg2 = zzbgm.zzg(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new DetectedActivity(iZzg, iZzg2);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ DetectedActivity[] newArray(int i) {
        return new DetectedActivity[i];
    }
}
