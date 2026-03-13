package com.google.firebase.iid;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzh implements ServiceConnection {
    private final Context zzaiq;
    private final Intent zzimp;
    private final ScheduledExecutorService zzimq;
    private final Queue<zzd> zzimr;
    private boolean zzimt;
    private zzf zzokm;

    public zzh(Context context, String str) {
        this(context, str, new ScheduledThreadPoolExecutor(0));
    }

    @VisibleForTesting
    private zzh(Context context, String str, ScheduledExecutorService scheduledExecutorService) {
        this.zzimr = new ArrayDeque();
        this.zzimt = false;
        this.zzaiq = context.getApplicationContext();
        this.zzimp = new Intent(str).setPackage(this.zzaiq.getPackageName());
        this.zzimq = scheduledExecutorService;
    }

    private final synchronized void zzawo() {
        if (Log.isLoggable("EnhancedIntentService", 3)) {
            Log.d("EnhancedIntentService", "flush queue called");
        }
        while (!this.zzimr.isEmpty()) {
            if (Log.isLoggable("EnhancedIntentService", 3)) {
                Log.d("EnhancedIntentService", "found intent to be delivered");
            }
            if (this.zzokm == null || !this.zzokm.isBinderAlive()) {
                if (Log.isLoggable("EnhancedIntentService", 3)) {
                    boolean z = !this.zzimt;
                    StringBuilder sb = new StringBuilder(39);
                    sb.append("binder is dead. start connection? ");
                    sb.append(z);
                    Log.d("EnhancedIntentService", sb.toString());
                }
                if (!this.zzimt) {
                    this.zzimt = true;
                    try {
                        if (com.google.android.gms.common.stats.zza.zzanm().zza(this.zzaiq, this.zzimp, this, 65)) {
                            return;
                        } else {
                            Log.e("EnhancedIntentService", "binding to the service failed");
                        }
                    } catch (SecurityException e) {
                        Log.e("EnhancedIntentService", "Exception while binding the service", e);
                    }
                    while (!this.zzimr.isEmpty()) {
                        this.zzimr.poll().finish();
                    }
                }
                return;
            }
            if (Log.isLoggable("EnhancedIntentService", 3)) {
                Log.d("EnhancedIntentService", "binder is alive, sending the intent.");
            }
            this.zzokm.zza(this.zzimr.poll());
        }
    }

    @Override // android.content.ServiceConnection
    public final void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        synchronized (this) {
            this.zzimt = false;
            this.zzokm = (zzf) iBinder;
            if (Log.isLoggable("EnhancedIntentService", 3)) {
                String strValueOf = String.valueOf(componentName);
                StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 20);
                sb.append("onServiceConnected: ");
                sb.append(strValueOf);
                Log.d("EnhancedIntentService", sb.toString());
            }
            zzawo();
        }
    }

    @Override // android.content.ServiceConnection
    public final void onServiceDisconnected(ComponentName componentName) {
        if (Log.isLoggable("EnhancedIntentService", 3)) {
            String strValueOf = String.valueOf(componentName);
            StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 23);
            sb.append("onServiceDisconnected: ");
            sb.append(strValueOf);
            Log.d("EnhancedIntentService", sb.toString());
        }
        zzawo();
    }

    public final synchronized void zza(Intent intent, BroadcastReceiver.PendingResult pendingResult) {
        if (Log.isLoggable("EnhancedIntentService", 3)) {
            Log.d("EnhancedIntentService", "new intent queued in the bind-strategy delivery");
        }
        this.zzimr.add(new zzd(intent, pendingResult, this.zzimq));
        zzawo();
    }
}
