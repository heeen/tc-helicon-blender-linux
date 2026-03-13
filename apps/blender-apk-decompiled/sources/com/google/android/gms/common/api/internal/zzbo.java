package com.google.android.gms.common.api.internal;

import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Api.ApiOptions;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.internal.zzcyj;
import com.google.android.gms.tasks.TaskCompletionSource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class zzbo<O extends Api.ApiOptions> implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, zzu {
    private final zzh<O> zzfsn;
    private final Api.zze zzfwd;
    private boolean zzfye;
    final /* synthetic */ zzbm zzfzq;
    private final Api.zzb zzfzs;
    private final zzae zzfzt;
    private final int zzfzw;
    private final zzcv zzfzx;
    private final Queue<zza> zzfzr = new LinkedList();
    private final Set<zzj> zzfzu = new HashSet();
    private final Map<zzck<?>, zzcr> zzfzv = new HashMap();
    private int zzfzy = -1;
    private ConnectionResult zzfzz = null;

    @WorkerThread
    public zzbo(zzbm zzbmVar, GoogleApi<O> googleApi) {
        this.zzfzq = zzbmVar;
        this.zzfwd = googleApi.zza(zzbmVar.mHandler.getLooper(), this);
        this.zzfzs = this.zzfwd instanceof com.google.android.gms.common.internal.zzbz ? com.google.android.gms.common.internal.zzbz.zzanb() : this.zzfwd;
        this.zzfsn = googleApi.zzahv();
        this.zzfzt = new zzae();
        this.zzfzw = googleApi.getInstanceId();
        if (this.zzfwd.zzacc()) {
            this.zzfzx = googleApi.zza(zzbmVar.mContext, zzbmVar.mHandler);
        } else {
            this.zzfzx = null;
        }
    }

    private final void zzake() {
        this.zzfzy = -1;
        this.zzfzq.zzfzk = -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @WorkerThread
    public final void zzakf() {
        zzaki();
        zzi(ConnectionResult.zzfqt);
        zzakk();
        Iterator<zzcr> it = this.zzfzv.values().iterator();
        while (it.hasNext()) {
            try {
                it.next().zzfty.zzb(this.zzfzs, new TaskCompletionSource<>());
            } catch (DeadObjectException unused) {
                onConnectionSuspended(1);
                this.zzfwd.disconnect();
            } catch (RemoteException unused2) {
            }
        }
        while (this.zzfwd.isConnected() && !this.zzfzr.isEmpty()) {
            zzb(this.zzfzr.remove());
        }
        zzakl();
    }

    /* JADX INFO: Access modifiers changed from: private */
    @WorkerThread
    public final void zzakg() {
        zzaki();
        this.zzfye = true;
        this.zzfzt.zzaje();
        this.zzfzq.mHandler.sendMessageDelayed(Message.obtain(this.zzfzq.mHandler, 9, this.zzfsn), this.zzfzq.zzfyg);
        this.zzfzq.mHandler.sendMessageDelayed(Message.obtain(this.zzfzq.mHandler, 11, this.zzfsn), this.zzfzq.zzfyf);
        zzake();
    }

    @WorkerThread
    private final void zzakk() {
        if (this.zzfye) {
            this.zzfzq.mHandler.removeMessages(11, this.zzfsn);
            this.zzfzq.mHandler.removeMessages(9, this.zzfsn);
            this.zzfye = false;
        }
    }

    private final void zzakl() {
        this.zzfzq.mHandler.removeMessages(12, this.zzfsn);
        this.zzfzq.mHandler.sendMessageDelayed(this.zzfzq.mHandler.obtainMessage(12, this.zzfsn), this.zzfzq.zzfzi);
    }

    @WorkerThread
    private final void zzb(zza zzaVar) {
        zzaVar.zza(this.zzfzt, zzacc());
        try {
            zzaVar.zza((zzbo<?>) this);
        } catch (DeadObjectException unused) {
            onConnectionSuspended(1);
            this.zzfwd.disconnect();
        }
    }

    @WorkerThread
    private final void zzi(ConnectionResult connectionResult) {
        for (zzj zzjVar : this.zzfzu) {
            String strZzahp = null;
            if (connectionResult == ConnectionResult.zzfqt) {
                strZzahp = this.zzfwd.zzahp();
            }
            zzjVar.zza(this.zzfsn, connectionResult, strZzahp);
        }
        this.zzfzu.clear();
    }

    @WorkerThread
    public final void connect() {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        if (this.zzfwd.isConnected() || this.zzfwd.isConnecting()) {
            return;
        }
        if (this.zzfwd.zzahn()) {
            this.zzfwd.zzahq();
            if (this.zzfzq.zzfzk != 0) {
                GoogleApiAvailability unused = this.zzfzq.zzftg;
                int iZzc = GoogleApiAvailability.zzc(this.zzfzq.mContext, this.zzfwd.zzahq());
                this.zzfwd.zzahq();
                this.zzfzq.zzfzk = iZzc;
                if (iZzc != 0) {
                    onConnectionFailed(new ConnectionResult(iZzc, null));
                    return;
                }
            }
        }
        zzbu zzbuVar = new zzbu(this.zzfzq, this.zzfwd, this.zzfsn);
        if (this.zzfwd.zzacc()) {
            this.zzfzx.zza(zzbuVar);
        }
        this.zzfwd.zza(zzbuVar);
    }

    public final int getInstanceId() {
        return this.zzfzw;
    }

    final boolean isConnected() {
        return this.zzfwd.isConnected();
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
    public final void onConnected(@Nullable Bundle bundle) {
        if (Looper.myLooper() == this.zzfzq.mHandler.getLooper()) {
            zzakf();
        } else {
            this.zzfzq.mHandler.post(new zzbp(this));
        }
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
    @WorkerThread
    public final void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        if (this.zzfzx != null) {
            this.zzfzx.zzakz();
        }
        zzaki();
        zzake();
        zzi(connectionResult);
        if (connectionResult.getErrorCode() == 4) {
            zzw(zzbm.zzfzh);
            return;
        }
        if (this.zzfzr.isEmpty()) {
            this.zzfzz = connectionResult;
            return;
        }
        synchronized (zzbm.sLock) {
            if (this.zzfzq.zzfzn != null && this.zzfzq.zzfzo.contains(this.zzfsn)) {
                this.zzfzq.zzfzn.zzb(connectionResult, this.zzfzw);
                return;
            }
            if (this.zzfzq.zzc(connectionResult, this.zzfzw)) {
                return;
            }
            if (connectionResult.getErrorCode() == 18) {
                this.zzfye = true;
            }
            if (this.zzfye) {
                this.zzfzq.mHandler.sendMessageDelayed(Message.obtain(this.zzfzq.mHandler, 9, this.zzfsn), this.zzfzq.zzfyg);
                return;
            }
            String strZzaig = this.zzfsn.zzaig();
            StringBuilder sb = new StringBuilder(String.valueOf(strZzaig).length() + 38);
            sb.append("API: ");
            sb.append(strZzaig);
            sb.append(" is not available on this device.");
            zzw(new Status(17, sb.toString()));
        }
    }

    @Override // com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
    public final void onConnectionSuspended(int i) {
        if (Looper.myLooper() == this.zzfzq.mHandler.getLooper()) {
            zzakg();
        } else {
            this.zzfzq.mHandler.post(new zzbq(this));
        }
    }

    @WorkerThread
    public final void resume() {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        if (this.zzfye) {
            connect();
        }
    }

    @WorkerThread
    public final void signOut() {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        zzw(zzbm.zzfzg);
        this.zzfzt.zzajd();
        for (zzck zzckVar : (zzck[]) this.zzfzv.keySet().toArray(new zzck[this.zzfzv.size()])) {
            zza(new zzf(zzckVar, new TaskCompletionSource()));
        }
        zzi(new ConnectionResult(4));
        if (this.zzfwd.isConnected()) {
            this.zzfwd.zza(new zzbs(this));
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzu
    public final void zza(ConnectionResult connectionResult, Api<?> api, boolean z) {
        if (Looper.myLooper() == this.zzfzq.mHandler.getLooper()) {
            onConnectionFailed(connectionResult);
        } else {
            this.zzfzq.mHandler.post(new zzbr(this, connectionResult));
        }
    }

    @WorkerThread
    public final void zza(zza zzaVar) {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        if (this.zzfwd.isConnected()) {
            zzb(zzaVar);
            zzakl();
            return;
        }
        this.zzfzr.add(zzaVar);
        if (this.zzfzz == null || !this.zzfzz.hasResolution()) {
            connect();
        } else {
            onConnectionFailed(this.zzfzz);
        }
    }

    @WorkerThread
    public final void zza(zzj zzjVar) {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        this.zzfzu.add(zzjVar);
    }

    public final boolean zzacc() {
        return this.zzfwd.zzacc();
    }

    public final Api.zze zzaix() {
        return this.zzfwd;
    }

    @WorkerThread
    public final void zzajr() {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        if (this.zzfye) {
            zzakk();
            zzw(this.zzfzq.zzftg.isGooglePlayServicesAvailable(this.zzfzq.mContext) == 18 ? new Status(8, "Connection timed out while waiting for Google Play services update to complete.") : new Status(8, "API failed to connect while resuming due to an unknown error."));
            this.zzfwd.disconnect();
        }
    }

    public final Map<zzck<?>, zzcr> zzakh() {
        return this.zzfzv;
    }

    @WorkerThread
    public final void zzaki() {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        this.zzfzz = null;
    }

    @WorkerThread
    public final ConnectionResult zzakj() {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        return this.zzfzz;
    }

    @WorkerThread
    public final void zzakm() {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        if (this.zzfwd.isConnected() && this.zzfzv.size() == 0) {
            if (this.zzfzt.zzajc()) {
                zzakl();
            } else {
                this.zzfwd.disconnect();
            }
        }
    }

    final zzcyj zzakn() {
        if (this.zzfzx == null) {
            return null;
        }
        return this.zzfzx.zzakn();
    }

    @WorkerThread
    public final void zzh(@NonNull ConnectionResult connectionResult) {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        this.zzfwd.disconnect();
        onConnectionFailed(connectionResult);
    }

    @WorkerThread
    public final void zzw(Status status) {
        com.google.android.gms.common.internal.zzbq.zza(this.zzfzq.mHandler);
        Iterator<zza> it = this.zzfzr.iterator();
        while (it.hasNext()) {
            it.next().zzs(status);
        }
        this.zzfzr.clear();
    }
}
