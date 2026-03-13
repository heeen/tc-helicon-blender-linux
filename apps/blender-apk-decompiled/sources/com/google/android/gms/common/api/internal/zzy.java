package com.google.android.gms.common.api.internal;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.gms.common.ConnectionResult;

/* JADX INFO: loaded from: classes.dex */
final class zzy implements zzcd {
    private /* synthetic */ zzv zzfwc;

    private zzy(zzv zzvVar) {
        this.zzfwc = zzvVar;
    }

    /* synthetic */ zzy(zzv zzvVar, zzw zzwVar) {
        this(zzvVar);
    }

    @Override // com.google.android.gms.common.api.internal.zzcd
    public final void zzc(@NonNull ConnectionResult connectionResult) {
        this.zzfwc.zzfwa.lock();
        try {
            this.zzfwc.zzfvy = connectionResult;
            this.zzfwc.zzait();
        } finally {
            this.zzfwc.zzfwa.unlock();
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzcd
    public final void zzf(int i, boolean z) {
        this.zzfwc.zzfwa.lock();
        try {
            if (this.zzfwc.zzfvz) {
                this.zzfwc.zzfvz = false;
                this.zzfwc.zze(i, z);
            } else {
                this.zzfwc.zzfvz = true;
                this.zzfwc.zzfvr.onConnectionSuspended(i);
            }
        } finally {
            this.zzfwc.zzfwa.unlock();
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzcd
    public final void zzk(@Nullable Bundle bundle) {
        this.zzfwc.zzfwa.lock();
        try {
            this.zzfwc.zzfvy = ConnectionResult.zzfqt;
            this.zzfwc.zzait();
        } finally {
            this.zzfwc.zzfwa.unlock();
        }
    }
}
