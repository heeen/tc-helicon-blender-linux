package com.google.android.gms.internal;

import android.location.Location;
import com.google.android.gms.common.api.internal.zzcl;
import com.google.android.gms.location.LocationListener;

/* JADX INFO: loaded from: classes.dex */
final class zzchg implements zzcl<LocationListener> {
    private /* synthetic */ Location zziuj;

    zzchg(zzchf zzchfVar, Location location) {
        this.zziuj = location;
    }

    @Override // com.google.android.gms.common.api.internal.zzcl
    public final void zzajh() {
    }

    @Override // com.google.android.gms.common.api.internal.zzcl
    public final /* synthetic */ void zzu(LocationListener locationListener) {
        locationListener.onLocationChanged(this.zziuj);
    }
}
