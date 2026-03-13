package com.google.android.gms.common;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzb implements Parcelable.Creator<ConnectionResult> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ ConnectionResult createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        PendingIntent pendingIntent = null;
        String strZzq = null;
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
                case 3:
                    pendingIntent = (PendingIntent) zzbgm.zza(parcel, i, PendingIntent.CREATOR);
                    break;
                case 4:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new ConnectionResult(iZzg, iZzg2, pendingIntent, strZzq);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ ConnectionResult[] newArray(int i) {
        return new ConnectionResult[i];
    }
}
