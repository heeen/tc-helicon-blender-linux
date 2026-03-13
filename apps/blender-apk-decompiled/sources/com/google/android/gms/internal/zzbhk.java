package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbhk implements Parcelable.Creator<zzbhj> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhj createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        zzbhl zzbhlVar = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    zzbhlVar = (zzbhl) zzbgm.zza(parcel, i, zzbhl.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzbhj(iZzg, zzbhlVar);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhj[] newArray(int i) {
        return new zzbhj[i];
    }
}
