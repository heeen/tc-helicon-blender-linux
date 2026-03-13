package com.google.android.gms.common;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zza implements ServiceConnection {
    private boolean zzfqr = false;
    private final BlockingQueue<IBinder> zzfqs = new LinkedBlockingQueue();

    @Override // android.content.ServiceConnection
    public final void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        this.zzfqs.add(iBinder);
    }

    @Override // android.content.ServiceConnection
    public final void onServiceDisconnected(ComponentName componentName) {
    }

    public final IBinder zza(long j, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        zzbq.zzgw("BlockingServiceConnection.getServiceWithTimeout() called on main thread");
        if (this.zzfqr) {
            throw new IllegalStateException("Cannot call get on this connection more than once");
        }
        this.zzfqr = true;
        IBinder iBinderPoll = this.zzfqs.poll(10000L, timeUnit);
        if (iBinderPoll != null) {
            return iBinderPoll;
        }
        throw new TimeoutException("Timed out waiting for the service connection");
    }

    public final IBinder zzahd() throws InterruptedException {
        zzbq.zzgw("BlockingServiceConnection.getService() called on main thread");
        if (this.zzfqr) {
            throw new IllegalStateException("Cannot call get on this connection more than once");
        }
        this.zzfqr = true;
        return this.zzfqs.take();
    }
}
