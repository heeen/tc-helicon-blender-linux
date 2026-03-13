package com.google.android.gms.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/* JADX INFO: loaded from: classes.dex */
final class zzbhh implements zzbhi {
    zzbhh() {
    }

    @Override // com.google.android.gms.internal.zzbhi
    public final ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
