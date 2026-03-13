package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzg implements Parcelable.Creator<ActivityTransitionResult> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ ActivityTransitionResult createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        ArrayList arrayListZzc = null;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            if ((65535 & i) != 1) {
                zzbgm.zzb(parcel, i);
            } else {
                arrayListZzc = zzbgm.zzc(parcel, i, ActivityTransitionEvent.CREATOR);
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new ActivityTransitionResult(arrayListZzc);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ ActivityTransitionResult[] newArray(int i) {
        return new ActivityTransitionResult[i];
    }
}
