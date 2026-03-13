package com.google.android.gms.common.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
final class zzaj implements ServiceConnection {
    private ComponentName mComponentName;
    private IBinder zzgfp;
    private boolean zzghb;
    private final zzah zzghc;
    private /* synthetic */ zzai zzghd;
    private final Set<ServiceConnection> zzgha = new HashSet();
    private int mState = 2;

    public zzaj(zzai zzaiVar, zzah zzahVar) {
        this.zzghd = zzaiVar;
        this.zzghc = zzahVar;
    }

    public final IBinder getBinder() {
        return this.zzgfp;
    }

    public final ComponentName getComponentName() {
        return this.mComponentName;
    }

    public final int getState() {
        return this.mState;
    }

    public final boolean isBound() {
        return this.zzghb;
    }

    @Override // android.content.ServiceConnection
    public final void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        synchronized (this.zzghd.zzggw) {
            this.zzghd.mHandler.removeMessages(1, this.zzghc);
            this.zzgfp = iBinder;
            this.mComponentName = componentName;
            Iterator<ServiceConnection> it = this.zzgha.iterator();
            while (it.hasNext()) {
                it.next().onServiceConnected(componentName, iBinder);
            }
            this.mState = 1;
        }
    }

    @Override // android.content.ServiceConnection
    public final void onServiceDisconnected(ComponentName componentName) {
        synchronized (this.zzghd.zzggw) {
            this.zzghd.mHandler.removeMessages(1, this.zzghc);
            this.zzgfp = null;
            this.mComponentName = componentName;
            Iterator<ServiceConnection> it = this.zzgha.iterator();
            while (it.hasNext()) {
                it.next().onServiceDisconnected(componentName);
            }
            this.mState = 2;
        }
    }

    public final void zza(ServiceConnection serviceConnection, String str) {
        com.google.android.gms.common.stats.zza unused = this.zzghd.zzggx;
        Context unused2 = this.zzghd.mApplicationContext;
        this.zzghc.zzcq(this.zzghd.mApplicationContext);
        this.zzgha.add(serviceConnection);
    }

    public final boolean zza(ServiceConnection serviceConnection) {
        return this.zzgha.contains(serviceConnection);
    }

    public final boolean zzamv() {
        return this.zzgha.isEmpty();
    }

    public final void zzb(ServiceConnection serviceConnection, String str) {
        com.google.android.gms.common.stats.zza unused = this.zzghd.zzggx;
        Context unused2 = this.zzghd.mApplicationContext;
        this.zzgha.remove(serviceConnection);
    }

    public final void zzgr(String str) {
        this.mState = 3;
        this.zzghb = this.zzghd.zzggx.zza(this.zzghd.mApplicationContext, str, this.zzghc.zzcq(this.zzghd.mApplicationContext), this, this.zzghc.zzamu());
        if (this.zzghb) {
            this.zzghd.mHandler.sendMessageDelayed(this.zzghd.mHandler.obtainMessage(1, this.zzghc), this.zzghd.zzggz);
        } else {
            this.mState = 2;
            try {
                com.google.android.gms.common.stats.zza unused = this.zzghd.zzggx;
                this.zzghd.mApplicationContext.unbindService(this);
            } catch (IllegalArgumentException unused2) {
            }
        }
    }

    public final void zzgs(String str) {
        this.zzghd.mHandler.removeMessages(1, this.zzghc);
        com.google.android.gms.common.stats.zza unused = this.zzghd.zzggx;
        this.zzghd.mApplicationContext.unbindService(this);
        this.zzghb = false;
        this.mState = 2;
    }
}
