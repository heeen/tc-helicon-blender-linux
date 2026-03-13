package com.google.android.gms.common.internal;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.support.annotation.BinderThread;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzn extends zze {
    private /* synthetic */ zzd zzgfk;
    private IBinder zzgfo;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    @BinderThread
    public zzn(zzd zzdVar, int i, IBinder iBinder, Bundle bundle) {
        super(zzdVar, i, bundle);
        this.zzgfk = zzdVar;
        this.zzgfo = iBinder;
    }

    @Override // com.google.android.gms.common.internal.zze
    protected final boolean zzama() {
        try {
            String interfaceDescriptor = this.zzgfo.getInterfaceDescriptor();
            if (!this.zzgfk.zzhn().equals(interfaceDescriptor)) {
                String strZzhn = this.zzgfk.zzhn();
                StringBuilder sb = new StringBuilder(String.valueOf(strZzhn).length() + 34 + String.valueOf(interfaceDescriptor).length());
                sb.append("service descriptor mismatch: ");
                sb.append(strZzhn);
                sb.append(" vs. ");
                sb.append(interfaceDescriptor);
                Log.e("GmsClient", sb.toString());
                return false;
            }
            IInterface iInterfaceZzd = this.zzgfk.zzd(this.zzgfo);
            if (iInterfaceZzd == null || !(this.zzgfk.zza(2, 4, iInterfaceZzd) || this.zzgfk.zza(3, 4, iInterfaceZzd))) {
                return false;
            }
            this.zzgfk.zzgff = null;
            Bundle bundleZzagp = this.zzgfk.zzagp();
            if (this.zzgfk.zzgfb == null) {
                return true;
            }
            this.zzgfk.zzgfb.onConnected(bundleZzagp);
            return true;
        } catch (RemoteException unused) {
            Log.w("GmsClient", "service probably died");
            return false;
        }
    }

    @Override // com.google.android.gms.common.internal.zze
    protected final void zzj(ConnectionResult connectionResult) {
        if (this.zzgfk.zzgfc != null) {
            this.zzgfk.zzgfc.onConnectionFailed(connectionResult);
        }
        this.zzgfk.onConnectionFailed(connectionResult);
    }
}
