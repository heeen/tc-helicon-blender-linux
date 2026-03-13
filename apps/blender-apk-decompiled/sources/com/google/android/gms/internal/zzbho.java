package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbho implements Parcelable.Creator<zzbhm> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhm createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        String strZzq = null;
        int iZzg2 = 0;
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
                    iZzg2 = zzbgm.zzg(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzbhm(iZzg, strZzq, iZzg2);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhm[] newArray(int i) {
        return new zzbhm[i];
    }
}
