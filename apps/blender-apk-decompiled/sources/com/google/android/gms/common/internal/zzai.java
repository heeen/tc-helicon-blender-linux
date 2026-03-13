package com.google.android.gms.common.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.Message;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;
import java.util.HashMap;

/* JADX INFO: loaded from: classes.dex */
final class zzai extends zzag implements Handler.Callback {
    private final Context mApplicationContext;
    private final Handler mHandler;
    private final HashMap<zzah, zzaj> zzggw = new HashMap<>();
    private final com.google.android.gms.common.stats.zza zzggx = com.google.android.gms.common.stats.zza.zzanm();
    private final long zzggy = 5000;
    private final long zzggz = 300000;

    zzai(Context context) {
        this.mApplicationContext = context.getApplicationContext();
        this.mHandler = new Handler(context.getMainLooper(), this);
    }

    @Override // android.os.Handler.Callback
    public final boolean handleMessage(Message message) {
        switch (message.what) {
            case 0:
                synchronized (this.zzggw) {
                    zzah zzahVar = (zzah) message.obj;
                    zzaj zzajVar = this.zzggw.get(zzahVar);
                    if (zzajVar != null && zzajVar.zzamv()) {
                        if (zzajVar.isBound()) {
                            zzajVar.zzgs("GmsClientSupervisor");
                        }
                        this.zzggw.remove(zzahVar);
                    }
                    break;
                }
                return true;
            case 1:
                synchronized (this.zzggw) {
                    zzah zzahVar2 = (zzah) message.obj;
                    zzaj zzajVar2 = this.zzggw.get(zzahVar2);
                    if (zzajVar2 != null && zzajVar2.getState() == 3) {
                        String strValueOf = String.valueOf(zzahVar2);
                        StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 47);
                        sb.append("Timeout waiting for ServiceConnection callback ");
                        sb.append(strValueOf);
                        Log.wtf("GmsClientSupervisor", sb.toString(), new Exception());
                        ComponentName componentName = zzajVar2.getComponentName();
                        if (componentName == null) {
                            componentName = zzahVar2.getComponentName();
                        }
                        if (componentName == null) {
                            componentName = new ComponentName(zzahVar2.getPackage(), EnvironmentCompat.MEDIA_UNKNOWN);
                        }
                        zzajVar2.onServiceDisconnected(componentName);
                    }
                    break;
                }
                return true;
            default:
                return false;
        }
    }

    @Override // com.google.android.gms.common.internal.zzag
    protected final boolean zza(zzah zzahVar, ServiceConnection serviceConnection, String str) {
        boolean zIsBound;
        zzbq.checkNotNull(serviceConnection, "ServiceConnection must not be null");
        synchronized (this.zzggw) {
            zzaj zzajVar = this.zzggw.get(zzahVar);
            if (zzajVar == null) {
                zzajVar = new zzaj(this, zzahVar);
                zzajVar.zza(serviceConnection, str);
                zzajVar.zzgr(str);
                this.zzggw.put(zzahVar, zzajVar);
            } else {
                this.mHandler.removeMessages(0, zzahVar);
                if (!zzajVar.zza(serviceConnection)) {
                    zzajVar.zza(serviceConnection, str);
                    switch (zzajVar.getState()) {
                        case 1:
                            serviceConnection.onServiceConnected(zzajVar.getComponentName(), zzajVar.getBinder());
                            break;
                        case 2:
                            zzajVar.zzgr(str);
                            break;
                    }
                } else {
                    String strValueOf = String.valueOf(zzahVar);
                    StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 81);
                    sb.append("Trying to bind a GmsServiceConnection that was already connected before.  config=");
                    sb.append(strValueOf);
                    throw new IllegalStateException(sb.toString());
                }
            }
            zIsBound = zzajVar.isBound();
        }
        return zIsBound;
    }

    @Override // com.google.android.gms.common.internal.zzag
    protected final void zzb(zzah zzahVar, ServiceConnection serviceConnection, String str) {
        zzbq.checkNotNull(serviceConnection, "ServiceConnection must not be null");
        synchronized (this.zzggw) {
            zzaj zzajVar = this.zzggw.get(zzahVar);
            if (zzajVar == null) {
                String strValueOf = String.valueOf(zzahVar);
                StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 50);
                sb.append("Nonexistent connection status for service config: ");
                sb.append(strValueOf);
                throw new IllegalStateException(sb.toString());
            }
            if (!zzajVar.zza(serviceConnection)) {
                String strValueOf2 = String.valueOf(zzahVar);
                StringBuilder sb2 = new StringBuilder(String.valueOf(strValueOf2).length() + 76);
                sb2.append("Trying to unbind a GmsServiceConnection  that was not bound before.  config=");
                sb2.append(strValueOf2);
                throw new IllegalStateException(sb2.toString());
            }
            zzajVar.zzb(serviceConnection, str);
            if (zzajVar.zzamv()) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0, zzahVar), this.zzggy);
            }
        }
    }
}
