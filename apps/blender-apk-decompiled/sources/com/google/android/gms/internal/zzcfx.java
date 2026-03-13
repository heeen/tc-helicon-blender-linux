package com.google.android.gms.internal;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcfx implements Parcelable.Creator<zzcfw> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcfw createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        zzcfu zzcfuVar = null;
        int iZzg = 1;
        IBinder iBinderZzr = null;
        IBinder iBinderZzr2 = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    zzcfuVar = (zzcfu) zzbgm.zza(parcel, i, zzcfu.CREATOR);
                    break;
                case 3:
                    iBinderZzr = zzbgm.zzr(parcel, i);
                    break;
                case 4:
                    iBinderZzr2 = zzbgm.zzr(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzcfw(iZzg, zzcfuVar, iBinderZzr, iBinderZzr2);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcfw[] newArray(int i) {
        return new zzcfw[i];
    }
}
