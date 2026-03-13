package com.google.android.gms.internal;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: loaded from: classes.dex */
final class zzdys {
    private final ConcurrentHashMap<zzdyt, List<Throwable>> zzmmi = new ConcurrentHashMap<>(16, 0.75f, 10);
    private final ReferenceQueue<Throwable> zzmmj = new ReferenceQueue<>();

    zzdys() {
    }

    public final List<Throwable> zza(Throwable th, boolean z) {
        while (true) {
            Reference<? extends Throwable> referencePoll = this.zzmmj.poll();
            if (referencePoll == null) {
                return this.zzmmi.get(new zzdyt(th, null));
            }
            this.zzmmi.remove(referencePoll);
        }
    }
}
