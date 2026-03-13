package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcfv implements Parcelable.Creator<zzcfu> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcfu createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        com.google.android.gms.location.zzj zzjVar = zzcfu.zzitn;
        List<zzcfs> listZzc = zzcfu.zzitm;
        String strZzq = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    zzjVar = (com.google.android.gms.location.zzj) zzbgm.zza(parcel, i, com.google.android.gms.location.zzj.CREATOR);
                    break;
                case 2:
                    listZzc = zzbgm.zzc(parcel, i, zzcfs.CREATOR);
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
        return new zzcfu(zzjVar, listZzc, strZzq);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzcfu[] newArray(int i) {
        return new zzcfu[i];
    }
}
