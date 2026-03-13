package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbhz implements Parcelable.Creator<zzbhw> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhw createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        String strZzq = null;
        int iZzg = 0;
        ArrayList arrayListZzc = null;
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
                    arrayListZzc = zzbgm.zzc(parcel, i, zzbhx.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzbhw(iZzg, strZzq, arrayListZzc);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhw[] newArray(int i) {
        return new zzbhw[i];
    }
}
