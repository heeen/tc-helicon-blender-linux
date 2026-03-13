package com.google.firebase.messaging;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzf implements Parcelable.Creator<RemoteMessage> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ RemoteMessage createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        Bundle bundleZzs = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            if ((65535 & i) != 2) {
                zzbgm.zzb(parcel, i);
            } else {
                bundleZzs = zzbgm.zzs(parcel, i);
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new RemoteMessage(bundleZzs);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ RemoteMessage[] newArray(int i) {
        return new RemoteMessage[i];
    }
}
