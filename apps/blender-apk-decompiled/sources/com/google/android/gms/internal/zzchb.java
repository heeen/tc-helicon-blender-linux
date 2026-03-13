package com.google.android.gms.internal;

import com.google.android.gms.common.api.internal.zzci;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

/* JADX INFO: loaded from: classes.dex */
final class zzchb extends com.google.android.gms.location.zzv {
    private final zzci<LocationCallback> zzgbb;

    zzchb(zzci<LocationCallback> zzciVar) {
        this.zzgbb = zzciVar;
    }

    @Override // com.google.android.gms.location.zzu
    public final void onLocationAvailability(LocationAvailability locationAvailability) {
        this.zzgbb.zza(new zzchd(this, locationAvailability));
    }

    @Override // com.google.android.gms.location.zzu
    public final void onLocationResult(LocationResult locationResult) {
        this.zzgbb.zza(new zzchc(this, locationResult));
    }

    public final synchronized void release() {
        this.zzgbb.clear();
    }
}
