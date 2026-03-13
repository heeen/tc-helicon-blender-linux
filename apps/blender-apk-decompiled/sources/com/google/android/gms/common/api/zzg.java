package com.google.android.gms.common.api;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzg implements Parcelable.Creator<Status> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ Status createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        String strZzq = null;
        PendingIntent pendingIntent = null;
        int iZzg2 = 0;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            int i2 = 65535 & i;
            if (i2 != 1000) {
                switch (i2) {
                    case 1:
                        iZzg2 = zzbgm.zzg(parcel, i);
                        break;
                    case 2:
                        strZzq = zzbgm.zzq(parcel, i);
                        break;
                    case 3:
                        pendingIntent = (PendingIntent) zzbgm.zza(parcel, i, PendingIntent.CREATOR);
                        break;
                    default:
                        zzbgm.zzb(parcel, i);
                        break;
                }
            } else {
                iZzg = zzbgm.zzg(parcel, i);
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new Status(iZzg, iZzg2, strZzq, pendingIntent);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ Status[] newArray(int i) {
        return new Status[i];
    }
}
