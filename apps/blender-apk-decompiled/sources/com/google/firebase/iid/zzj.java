package com.google.firebase.iid;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes.dex */
final class zzj implements Parcelable.Creator<zzi> {
    zzj() {
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzi createFromParcel(Parcel parcel) {
        IBinder strongBinder = parcel.readStrongBinder();
        if (strongBinder != null) {
            return new zzi(strongBinder);
        }
        return null;
    }

    @Override // android.os.Parcelable.Creator
    public final /* synthetic */ zzi[] newArray(int i) {
        return new zzi[i];
    }
}
