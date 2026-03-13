package com.google.android.gms.internal;

import android.app.PendingIntent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcho implements Parcelable.Creator<zzchn> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzchn createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        zzchl zzchlVar = null;
        IBinder iBinderZzr = null;
        PendingIntent pendingIntent = null;
        IBinder iBinderZzr2 = null;
        IBinder iBinderZzr3 = null;
        int iZzg = 1;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    zzchlVar = (zzchl) zzbgm.zza(parcel, i, zzchl.CREATOR);
                    break;
                case 3:
                    iBinderZzr = zzbgm.zzr(parcel, i);
                    break;
                case 4:
                    pendingIntent = (PendingIntent) zzbgm.zza(parcel, i, PendingIntent.CREATOR);
                    break;
                case 5:
                    iBinderZzr2 = zzbgm.zzr(parcel, i);
                    break;
                case 6:
                    iBinderZzr3 = zzbgm.zzr(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzchn(iZzg, zzchlVar, iBinderZzr, pendingIntent, iBinderZzr2, iBinderZzr3);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzchn[] newArray(int i) {
        return new zzchn[i];
    }
}
