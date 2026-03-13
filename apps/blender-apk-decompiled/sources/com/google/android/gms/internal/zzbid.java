package com.google.android.gms.internal;

import com.google.android.gms.common.internal.zzbq;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: classes.dex */
public final class zzbid implements ThreadFactory {
    private final int mPriority;
    private final String zzgln;
    private final AtomicInteger zzglo;
    private final ThreadFactory zzglp;

    public zzbid(String str) {
        this(str, 0);
    }

    private zzbid(String str, int i) {
        this.zzglo = new AtomicInteger();
        this.zzglp = Executors.defaultThreadFactory();
        this.zzgln = (String) zzbq.checkNotNull(str, "Name must not be null");
        this.mPriority = 0;
    }

    @Override // java.util.concurrent.ThreadFactory
    public final Thread newThread(Runnable runnable) {
        Thread threadNewThread = this.zzglp.newThread(new zzbie(runnable, 0));
        String str = this.zzgln;
        int andIncrement = this.zzglo.getAndIncrement();
        StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 13);
        sb.append(str);
        sb.append("[");
        sb.append(andIncrement);
        sb.append("]");
        threadNewThread.setName(sb.toString());
        return threadNewThread;
    }
}
