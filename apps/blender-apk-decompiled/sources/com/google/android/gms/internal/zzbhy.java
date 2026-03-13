package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbhy implements Parcelable.Creator<zzbhv> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhv createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        ArrayList arrayListZzc = null;
        int iZzg = 0;
        String strZzq = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    arrayListZzc = zzbgm.zzc(parcel, i, zzbhw.CREATOR);
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
        return new zzbhv(iZzg, arrayListZzc, strZzq);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhv[] newArray(int i) {
        return new zzbhv[i];
    }
}
