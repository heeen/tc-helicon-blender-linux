package com.flurry.sdk;

import android.app.Activity;
import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes.dex */
public final class ju extends jz {
    public WeakReference<Activity> a;
    public a b;

    public enum a {
        kCreated,
        kDestroyed,
        kPaused,
        kResumed,
        kStarted,
        kStopped,
        kSaveState
    }

    public ju() {
        super("com.flurry.android.sdk.ActivityLifecycleEvent");
        this.a = new WeakReference<>(null);
    }
}
