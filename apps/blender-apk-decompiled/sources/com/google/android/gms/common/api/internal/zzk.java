package com.google.android.gms.common.api.internal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: loaded from: classes.dex */
public final class zzk implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
    private static final zzk zzfuo = new zzk();
    private final AtomicBoolean zzfup = new AtomicBoolean();
    private final AtomicBoolean zzfuq = new AtomicBoolean();
    private final ArrayList<zzl> zzfur = new ArrayList<>();
    private boolean zzdyq = false;

    private zzk() {
    }

    public static void zza(Application application) {
        synchronized (zzfuo) {
            if (!zzfuo.zzdyq) {
                application.registerActivityLifecycleCallbacks(zzfuo);
                application.registerComponentCallbacks(zzfuo);
                zzfuo.zzdyq = true;
            }
        }
    }

    public static zzk zzaij() {
        return zzfuo;
    }

    private final void zzbj(boolean z) {
        synchronized (zzfuo) {
            ArrayList<zzl> arrayList = this.zzfur;
            int size = arrayList.size();
            int i = 0;
            while (i < size) {
                zzl zzlVar = arrayList.get(i);
                i++;
                zzlVar.zzbj(z);
            }
        }
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityCreated(Activity activity, Bundle bundle) {
        boolean zCompareAndSet = this.zzfup.compareAndSet(true, false);
        this.zzfuq.set(true);
        if (zCompareAndSet) {
            zzbj(false);
        }
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityDestroyed(Activity activity) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityPaused(Activity activity) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityResumed(Activity activity) {
        boolean zCompareAndSet = this.zzfup.compareAndSet(true, false);
        this.zzfuq.set(true);
        if (zCompareAndSet) {
            zzbj(false);
        }
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityStarted(Activity activity) {
    }

    @Override // android.app.Application.ActivityLifecycleCallbacks
    public final void onActivityStopped(Activity activity) {
    }

    @Override // android.content.ComponentCallbacks
    public final void onConfigurationChanged(Configuration configuration) {
    }

    @Override // android.content.ComponentCallbacks
    public final void onLowMemory() {
    }

    @Override // android.content.ComponentCallbacks2
    public final void onTrimMemory(int i) {
        if (i == 20 && this.zzfup.compareAndSet(false, true)) {
            this.zzfuq.set(true);
            zzbj(true);
        }
    }

    public final void zza(zzl zzlVar) {
        synchronized (zzfuo) {
            this.zzfur.add(zzlVar);
        }
    }

    public final boolean zzaik() {
        return this.zzfup.get();
    }

    @TargetApi(16)
    public final boolean zzbi(boolean z) {
        if (!this.zzfuq.get()) {
            if (!com.google.android.gms.common.util.zzs.zzans()) {
                return true;
            }
            ActivityManager.RunningAppProcessInfo runningAppProcessInfo = new ActivityManager.RunningAppProcessInfo();
            ActivityManager.getMyMemoryState(runningAppProcessInfo);
            if (!this.zzfuq.getAndSet(true) && runningAppProcessInfo.importance > 100) {
                this.zzfup.set(true);
            }
        }
        return this.zzfup.get();
    }
}
