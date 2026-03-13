package com.google.android.gms.internal;

import android.app.PendingIntent;
import android.util.Log;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationStatusCodes;

/* JADX INFO: loaded from: classes.dex */
final class zzchi extends zzcgv {
    private com.google.android.gms.common.api.internal.zzn<Status> zziul;

    public zzchi(com.google.android.gms.common.api.internal.zzn<Status> zznVar) {
        this.zziul = zznVar;
    }

    @Override // com.google.android.gms.internal.zzcgu
    public final void zza(int i, PendingIntent pendingIntent) {
        Log.wtf("LocationClientImpl", "Unexpected call to onRemoveGeofencesByPendingIntentResult");
    }

    @Override // com.google.android.gms.internal.zzcgu
    public final void zza(int i, String[] strArr) {
        if (this.zziul == null) {
            Log.wtf("LocationClientImpl", "onAddGeofenceResult called multiple times");
            return;
        }
        this.zziul.setResult(LocationStatusCodes.zzek(LocationStatusCodes.zzej(i)));
        this.zziul = null;
    }

    @Override // com.google.android.gms.internal.zzcgu
    public final void zzb(int i, String[] strArr) {
        Log.wtf("LocationClientImpl", "Unexpected call to onRemoveGeofencesByRequestIdsResult");
    }
}
