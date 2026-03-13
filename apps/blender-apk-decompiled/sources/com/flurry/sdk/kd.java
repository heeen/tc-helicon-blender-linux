package com.flurry.sdk;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/* JADX INFO: loaded from: classes.dex */
public final class kd<V> extends FutureTask<V> {
    private final WeakReference<Callable<V>> a;
    private final WeakReference<Runnable> b;

    public kd(Runnable runnable, V v) {
        super(runnable, v);
        this.a = new WeakReference<>(null);
        this.b = new WeakReference<>(runnable);
    }

    public final Runnable a() {
        return this.b.get();
    }
}
