package com.google.android.gms.common.internal;

import android.app.PendingIntent;
import android.os.Handler;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;

/* JADX INFO: loaded from: classes.dex */
@Hide
final class zzh extends Handler {
    private /* synthetic */ zzd zzgfk;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public zzh(zzd zzdVar, Looper looper) {
        super(looper);
        this.zzgfk = zzdVar;
    }

    private static void zza(Message message) {
        zzi zziVar = (zzi) message.obj;
        zziVar.zzamb();
        zziVar.unregister();
    }

    private static boolean zzb(Message message) {
        return message.what == 2 || message.what == 1 || message.what == 7;
    }

    @Override // android.os.Handler
    public final void handleMessage(Message message) {
        if (this.zzgfk.zzgfh.get() != message.arg1) {
            if (zzb(message)) {
                zza(message);
                return;
            }
            return;
        }
        if ((message.what == 1 || message.what == 7 || message.what == 4 || message.what == 5) && !this.zzgfk.isConnecting()) {
            zza(message);
            return;
        }
        if (message.what == 4) {
            this.zzgfk.zzgff = new ConnectionResult(message.arg2);
            if (this.zzgfk.zzalz() && !this.zzgfk.zzgfg) {
                this.zzgfk.zza(3, (IInterface) null);
                return;
            }
            ConnectionResult connectionResult = this.zzgfk.zzgff != null ? this.zzgfk.zzgff : new ConnectionResult(8);
            this.zzgfk.zzgew.zzf(connectionResult);
            this.zzgfk.onConnectionFailed(connectionResult);
            return;
        }
        if (message.what == 5) {
            ConnectionResult connectionResult2 = this.zzgfk.zzgff != null ? this.zzgfk.zzgff : new ConnectionResult(8);
            this.zzgfk.zzgew.zzf(connectionResult2);
            this.zzgfk.onConnectionFailed(connectionResult2);
            return;
        }
        if (message.what == 3) {
            ConnectionResult connectionResult3 = new ConnectionResult(message.arg2, message.obj instanceof PendingIntent ? (PendingIntent) message.obj : null);
            this.zzgfk.zzgew.zzf(connectionResult3);
            this.zzgfk.onConnectionFailed(connectionResult3);
            return;
        }
        if (message.what == 6) {
            this.zzgfk.zza(5, (IInterface) null);
            if (this.zzgfk.zzgfb != null) {
                this.zzgfk.zzgfb.onConnectionSuspended(message.arg2);
            }
            this.zzgfk.onConnectionSuspended(message.arg2);
            this.zzgfk.zza(5, 1, (IInterface) null);
            return;
        }
        if (message.what == 2 && !this.zzgfk.isConnected()) {
            zza(message);
            return;
        }
        if (zzb(message)) {
            ((zzi) message.obj).zzamc();
            return;
        }
        int i = message.what;
        StringBuilder sb = new StringBuilder(45);
        sb.append("Don't know how to handle message: ");
        sb.append(i);
        Log.wtf("GmsClient", sb.toString(), new Exception());
    }
}
