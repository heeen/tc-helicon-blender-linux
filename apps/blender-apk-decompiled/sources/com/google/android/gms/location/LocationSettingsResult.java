package com.google.android.gms.location;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;

/* JADX INFO: loaded from: classes.dex */
public final class LocationSettingsResult extends zzbgl implements Result {
    public static final Parcelable.Creator<LocationSettingsResult> CREATOR = new zzah();
    private final Status mStatus;
    private final LocationSettingsStates zzisu;

    @Hide
    public LocationSettingsResult(Status status) {
        this(status, null);
    }

    @Hide
    public LocationSettingsResult(Status status, LocationSettingsStates locationSettingsStates) {
        this.mStatus = status;
        this.zzisu = locationSettingsStates;
    }

    public final LocationSettingsStates getLocationSettingsStates() {
        return this.zzisu;
    }

    @Override // com.google.android.gms.common.api.Result
    public final Status getStatus() {
        return this.mStatus;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 1, (Parcelable) getStatus(), i, false);
        zzbgo.zza(parcel, 2, (Parcelable) getLocationSettingsStates(), i, false);
        zzbgo.zzai(parcel, iZze);
    }
}
