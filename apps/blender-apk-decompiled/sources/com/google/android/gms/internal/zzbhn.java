package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzbhn implements Parcelable.Creator<zzbhl> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhl createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        int iZzg = 0;
        ArrayList arrayListZzc = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 2:
                    arrayListZzc = zzbgm.zzc(parcel, i, zzbhm.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new zzbhl(iZzg, arrayListZzc);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzbhl[] newArray(int i) {
        return new zzbhl[i];
    }
}
