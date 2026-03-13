package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;
import com.google.android.gms.internal.zzcfs;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzf implements Parcelable.Creator<ActivityTransitionRequest> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ ActivityTransitionRequest createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        ArrayList arrayListZzc = null;
        String strZzq = null;
        ArrayList arrayListZzc2 = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    arrayListZzc = zzbgm.zzc(parcel, i, ActivityTransition.CREATOR);
                    break;
                case 2:
                    strZzq = zzbgm.zzq(parcel, i);
                    break;
                case 3:
                    arrayListZzc2 = zzbgm.zzc(parcel, i, zzcfs.CREATOR);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new ActivityTransitionRequest(arrayListZzc, strZzq, arrayListZzc2);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ ActivityTransitionRequest[] newArray(int i) {
        return new ActivityTransitionRequest[i];
    }
}
