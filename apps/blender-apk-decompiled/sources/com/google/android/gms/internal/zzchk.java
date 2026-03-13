package com.google.android.gms.internal;

import android.os.RemoteException;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.location.LocationSettingsResult;

/* JADX INFO: loaded from: classes.dex */
final class zzchk extends zzcgz {
    private com.google.android.gms.common.api.internal.zzn<LocationSettingsResult> zziul;

    public zzchk(com.google.android.gms.common.api.internal.zzn<LocationSettingsResult> zznVar) {
        zzbq.checkArgument(zznVar != null, "listener can't be null.");
        this.zziul = zznVar;
    }

    @Override // com.google.android.gms.internal.zzcgy
    public final void zza(LocationSettingsResult locationSettingsResult) throws RemoteException {
        this.zziul.setResult(locationSettingsResult);
        this.zziul = null;
    }
}
