package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcgm implements Parcelable.Creator<zzcgl> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcgl createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        Status status = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            if ((65535 & i) != 1) {
                zzbgm.zzb(parcel, i);
            } else {
                status = (Status) zzbgm.zza(parcel, i, Status.CREATOR);
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzcgl(status);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcgl[] newArray(int i) {
        return new zzcgl[i];
    }
}
