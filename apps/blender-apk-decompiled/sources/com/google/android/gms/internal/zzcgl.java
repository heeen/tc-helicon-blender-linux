package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcgl extends zzbgl implements Result {
    private final Status mStatus;

    @Hide
    private static zzcgl zzitz = new zzcgl(Status.zzftq);
    public static final Parcelable.Creator<zzcgl> CREATOR = new zzcgm();

    @Hide
    public zzcgl(Status status) {
        this.mStatus = status;
    }

    @Override // com.google.android.gms.common.api.Result
    public final Status getStatus() {
        return this.mStatus;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 1, (Parcelable) getStatus(), i, false);
        zzbgo.zzai(parcel, iZze);
    }
}
