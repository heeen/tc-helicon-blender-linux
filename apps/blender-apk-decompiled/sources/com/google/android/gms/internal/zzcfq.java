package com.google.android.gms.internal;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public class zzcfq extends com.google.android.gms.common.internal.zzab<zzcgw> {
    private final String zzitj;
    protected final zzchr<zzcgw> zzitk;

    public zzcfq(Context context, Looper looper, GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener, String str, com.google.android.gms.common.internal.zzr zzrVar) {
        super(context, looper, 23, zzrVar, connectionCallbacks, onConnectionFailedListener);
        this.zzitk = new zzcfr(this);
        this.zzitj = str;
    }

    @Override // com.google.android.gms.common.internal.zzd
    protected final Bundle zzabt() {
        Bundle bundle = new Bundle();
        bundle.putString("client_name", this.zzitj);
        return bundle;
    }

    @Override // com.google.android.gms.common.internal.zzd
    protected final /* synthetic */ IInterface zzd(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.location.internal.IGoogleLocationManagerService");
        return iInterfaceQueryLocalInterface instanceof zzcgw ? (zzcgw) iInterfaceQueryLocalInterface : new zzcgx(iBinder);
    }

    @Override // com.google.android.gms.common.internal.zzd
    protected final String zzhm() {
        return "com.google.android.location.internal.GoogleLocationManagerService.START";
    }

    @Override // com.google.android.gms.common.internal.zzd
    protected final String zzhn() {
        return "com.google.android.gms.location.internal.IGoogleLocationManagerService";
    }
}
