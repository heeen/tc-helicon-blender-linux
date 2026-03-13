package com.google.android.gms.internal;

import com.google.android.gms.common.api.internal.zzcl;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;

/* JADX INFO: loaded from: classes.dex */
final class zzchd implements zzcl<LocationCallback> {
    private /* synthetic */ LocationAvailability zziui;

    zzchd(zzchb zzchbVar, LocationAvailability locationAvailability) {
        this.zziui = locationAvailability;
    }

    @Override // com.google.android.gms.common.api.internal.zzcl
    public final void zzajh() {
    }

    @Override // com.google.android.gms.common.api.internal.zzcl
    public final /* synthetic */ void zzu(LocationCallback locationCallback) {
        locationCallback.onLocationAvailability(this.zziui);
    }
}
