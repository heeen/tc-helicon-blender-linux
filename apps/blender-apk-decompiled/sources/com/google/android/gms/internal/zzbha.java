package com.google.android.gms.internal;

import android.content.Context;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import com.google.android.gms.common.api.GoogleApiClient;

/* JADX INFO: loaded from: classes.dex */
public final class zzbha extends com.google.android.gms.common.internal.zzab<zzbhd> {
    public zzbha(Context context, Looper looper, com.google.android.gms.common.internal.zzr zzrVar, GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        super(context, looper, 39, zzrVar, connectionCallbacks, onConnectionFailedListener);
    }

    @Override // com.google.android.gms.common.internal.zzd
    protected final /* synthetic */ IInterface zzd(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.common.internal.service.ICommonService");
        return iInterfaceQueryLocalInterface instanceof zzbhd ? (zzbhd) iInterfaceQueryLocalInterface : new zzbhe(iBinder);
    }

    @Override // com.google.android.gms.common.internal.zzd
    public final String zzhm() {
        return "com.google.android.gms.common.service.START";
    }

    @Override // com.google.android.gms.common.internal.zzd
    protected final String zzhn() {
        return "com.google.android.gms.common.internal.service.ICommonService";
    }
}
