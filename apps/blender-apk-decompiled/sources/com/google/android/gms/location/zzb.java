package com.google.android.gms.location;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgm;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzb implements Parcelable.Creator<ActivityRecognitionResult> {
    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ ActivityRecognitionResult createFromParcel(Parcel parcel) {
        int iZzd = zzbgm.zzd(parcel);
        long jZzi = 0;
        long jZzi2 = 0;
        ArrayList arrayListZzc = null;
        Bundle bundleZzs = null;
        int iZzg = 0;
        while (parcel.dataPosition() < iZzd) {
            int i = parcel.readInt();
            switch (65535 & i) {
                case 1:
                    arrayListZzc = zzbgm.zzc(parcel, i, DetectedActivity.CREATOR);
                    break;
                case 2:
                    jZzi = zzbgm.zzi(parcel, i);
                    break;
                case 3:
                    jZzi2 = zzbgm.zzi(parcel, i);
                    break;
                case 4:
                    iZzg = zzbgm.zzg(parcel, i);
                    break;
                case 5:
                    bundleZzs = zzbgm.zzs(parcel, i);
                    break;
                default:
                    zzbgm.zzb(parcel, i);
                    break;
            }
        }
        zzbgm.zzaf(parcel, iZzd);
        return new ActivityRecognitionResult(arrayListZzc, jZzi, jZzi2, iZzg, bundleZzs);
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ ActivityRecognitionResult[] newArray(int i) {
        return new ActivityRecognitionResult[i];
    }
}
