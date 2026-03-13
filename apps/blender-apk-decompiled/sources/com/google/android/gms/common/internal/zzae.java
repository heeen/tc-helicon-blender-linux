package com.google.android.gms.common.internal;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: classes.dex */
public final class zzae implements Handler.Callback {
    private final Handler mHandler;
    private final zzaf zzggl;
    private final ArrayList<GoogleApiClient.ConnectionCallbacks> zzggm = new ArrayList<>();
    private ArrayList<GoogleApiClient.ConnectionCallbacks> zzggn = new ArrayList<>();
    private final ArrayList<GoogleApiClient.OnConnectionFailedListener> zzggo = new ArrayList<>();
    private volatile boolean zzggp = false;
    private final AtomicInteger zzggq = new AtomicInteger(0);
    private boolean zzggr = false;
    private final Object mLock = new Object();

    public zzae(Looper looper, zzaf zzafVar) {
        this.zzggl = zzafVar;
        this.mHandler = new Handler(looper, this);
    }

    @Override // android.os.Handler.Callback
    public final boolean handleMessage(Message message) {
        if (message.what != 1) {
            int i = message.what;
            StringBuilder sb = new StringBuilder(45);
            sb.append("Don't know how to handle message: ");
            sb.append(i);
            Log.wtf("GmsClientEvents", sb.toString(), new Exception());
            return false;
        }
        GoogleApiClient.ConnectionCallbacks connectionCallbacks = (GoogleApiClient.ConnectionCallbacks) message.obj;
        synchronized (this.mLock) {
            if (this.zzggp && this.zzggl.isConnected() && this.zzggm.contains(connectionCallbacks)) {
                connectionCallbacks.onConnected(this.zzggl.zzagp());
            }
        }
        return true;
    }

    public final boolean isConnectionCallbacksRegistered(GoogleApiClient.ConnectionCallbacks connectionCallbacks) {
        boolean zContains;
        zzbq.checkNotNull(connectionCallbacks);
        synchronized (this.mLock) {
            zContains = this.zzggm.contains(connectionCallbacks);
        }
        return zContains;
    }

    public final boolean isConnectionFailedListenerRegistered(GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        boolean zContains;
        zzbq.checkNotNull(onConnectionFailedListener);
        synchronized (this.mLock) {
            zContains = this.zzggo.contains(onConnectionFailedListener);
        }
        return zContains;
    }

    public final void registerConnectionCallbacks(GoogleApiClient.ConnectionCallbacks connectionCallbacks) {
        zzbq.checkNotNull(connectionCallbacks);
        synchronized (this.mLock) {
            if (this.zzggm.contains(connectionCallbacks)) {
                String strValueOf = String.valueOf(connectionCallbacks);
                StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 62);
                sb.append("registerConnectionCallbacks(): listener ");
                sb.append(strValueOf);
                sb.append(" is already registered");
                Log.w("GmsClientEvents", sb.toString());
            } else {
                this.zzggm.add(connectionCallbacks);
            }
        }
        if (this.zzggl.isConnected()) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1, connectionCallbacks));
        }
    }

    public final void registerConnectionFailedListener(GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        zzbq.checkNotNull(onConnectionFailedListener);
        synchronized (this.mLock) {
            if (this.zzggo.contains(onConnectionFailedListener)) {
                String strValueOf = String.valueOf(onConnectionFailedListener);
                StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 67);
                sb.append("registerConnectionFailedListener(): listener ");
                sb.append(strValueOf);
                sb.append(" is already registered");
                Log.w("GmsClientEvents", sb.toString());
            } else {
                this.zzggo.add(onConnectionFailedListener);
            }
        }
    }

    public final void unregisterConnectionCallbacks(GoogleApiClient.ConnectionCallbacks connectionCallbacks) {
        zzbq.checkNotNull(connectionCallbacks);
        synchronized (this.mLock) {
            if (!this.zzggm.remove(connectionCallbacks)) {
                String strValueOf = String.valueOf(connectionCallbacks);
                StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 52);
                sb.append("unregisterConnectionCallbacks(): listener ");
                sb.append(strValueOf);
                sb.append(" not found");
                Log.w("GmsClientEvents", sb.toString());
            } else if (this.zzggr) {
                this.zzggn.add(connectionCallbacks);
            }
        }
    }

    public final void unregisterConnectionFailedListener(GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        zzbq.checkNotNull(onConnectionFailedListener);
        synchronized (this.mLock) {
            if (!this.zzggo.remove(onConnectionFailedListener)) {
                String strValueOf = String.valueOf(onConnectionFailedListener);
                StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 57);
                sb.append("unregisterConnectionFailedListener(): listener ");
                sb.append(strValueOf);
                sb.append(" not found");
                Log.w("GmsClientEvents", sb.toString());
            }
        }
    }

    public final void zzams() {
        this.zzggp = false;
        this.zzggq.incrementAndGet();
    }

    public final void zzamt() {
        this.zzggp = true;
    }

    public final void zzcf(int i) {
        zzbq.zza(Looper.myLooper() == this.mHandler.getLooper(), "onUnintentionalDisconnection must only be called on the Handler thread");
        this.mHandler.removeMessages(1);
        synchronized (this.mLock) {
            this.zzggr = true;
            ArrayList arrayList = new ArrayList(this.zzggm);
            int i2 = this.zzggq.get();
            ArrayList arrayList2 = arrayList;
            int size = arrayList2.size();
            int i3 = 0;
            while (i3 < size) {
                Object obj = arrayList2.get(i3);
                i3++;
                GoogleApiClient.ConnectionCallbacks connectionCallbacks = (GoogleApiClient.ConnectionCallbacks) obj;
                if (!this.zzggp || this.zzggq.get() != i2) {
                    break;
                } else if (this.zzggm.contains(connectionCallbacks)) {
                    connectionCallbacks.onConnectionSuspended(i);
                }
            }
            this.zzggn.clear();
            this.zzggr = false;
        }
    }

    public final void zzk(ConnectionResult connectionResult) {
        int i = 0;
        zzbq.zza(Looper.myLooper() == this.mHandler.getLooper(), "onConnectionFailure must only be called on the Handler thread");
        this.mHandler.removeMessages(1);
        synchronized (this.mLock) {
            ArrayList arrayList = new ArrayList(this.zzggo);
            int i2 = this.zzggq.get();
            ArrayList arrayList2 = arrayList;
            int size = arrayList2.size();
            while (i < size) {
                Object obj = arrayList2.get(i);
                i++;
                GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener = (GoogleApiClient.OnConnectionFailedListener) obj;
                if (this.zzggp && this.zzggq.get() == i2) {
                    if (this.zzggo.contains(onConnectionFailedListener)) {
                        onConnectionFailedListener.onConnectionFailed(connectionResult);
                    }
                }
                return;
            }
        }
    }

    public final void zzl(Bundle bundle) {
        boolean z = true;
        zzbq.zza(Looper.myLooper() == this.mHandler.getLooper(), "onConnectionSuccess must only be called on the Handler thread");
        synchronized (this.mLock) {
            zzbq.checkState(!this.zzggr);
            this.mHandler.removeMessages(1);
            this.zzggr = true;
            if (this.zzggn.size() != 0) {
                z = false;
            }
            zzbq.checkState(z);
            ArrayList arrayList = new ArrayList(this.zzggm);
            int i = this.zzggq.get();
            ArrayList arrayList2 = arrayList;
            int size = arrayList2.size();
            int i2 = 0;
            while (i2 < size) {
                Object obj = arrayList2.get(i2);
                i2++;
                GoogleApiClient.ConnectionCallbacks connectionCallbacks = (GoogleApiClient.ConnectionCallbacks) obj;
                if (!this.zzggp || !this.zzggl.isConnected() || this.zzggq.get() != i) {
                    break;
                } else if (!this.zzggn.contains(connectionCallbacks)) {
                    connectionCallbacks.onConnected(bundle);
                }
            }
            this.zzggn.clear();
            this.zzggr = false;
        }
    }
}
