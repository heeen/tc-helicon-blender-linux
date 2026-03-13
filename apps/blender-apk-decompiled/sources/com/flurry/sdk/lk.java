package com.flurry.sdk;

import java.util.concurrent.ThreadFactory;

/* JADX INFO: loaded from: classes.dex */
public final class lk implements ThreadFactory {
    private final ThreadGroup a;
    private final int b = 1;

    public lk(String str) {
        this.a = new ThreadGroup(str);
    }

    @Override // java.util.concurrent.ThreadFactory
    public final Thread newThread(Runnable runnable) {
        Thread thread = new Thread(this.a, runnable);
        thread.setName(this.a.getName() + ":" + thread.getId());
        thread.setPriority(this.b);
        return thread;
    }
}
