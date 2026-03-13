package com.google.android.gms.common;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzo implements Parcelable.Creator<zzn> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzn createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        String strZzq = null;
        boolean zZzc = false;
        IBinder iBinderZzr = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 2:
                    iBinderZzr = zzbgm.zzr(parcel, i);
                    break;
                case 3:
                    zZzc = zzbgm.zzc(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzn(strZzq, iBinderZzr, zZzc);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzn[] newArray(int i) {
        return new zzn[i];
    }
}
