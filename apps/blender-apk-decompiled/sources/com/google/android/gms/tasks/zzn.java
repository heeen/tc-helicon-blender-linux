package com.google.android.gms.tasks;

import android.support.annotation.NonNull;
import java.util.ArrayDeque;
import java.util.Queue;

/* JADX INFO: loaded from: classes.dex */
final class zzn<TResult> {
    private final Object mLock = new Object();
    private Queue<zzm<TResult>> zzlej;
    private boolean zzlek;

    zzn() {
    }

    public final void zza(@NonNull zzm<TResult> zzmVar) {
        synchronized (this.mLock) {
            if (this.zzlej == null) {
                this.zzlej = new ArrayDeque();
            }
            this.zzlej.add(zzmVar);
        }
    }

    public final void zzb(@NonNull Task<TResult> task) {
        zzm<TResult> zzmVarPoll;
        synchronized (this.mLock) {
            if (this.zzlej != null && !this.zzlek) {
                this.zzlek = true;
                while (true) {
                    synchronized (this.mLock) {
                        zzmVarPoll = this.zzlej.poll();
                        if (zzmVarPoll == null) {
                            this.zzlek = false;
                            return;
                        }
                    }
                    zzmVarPoll.onComplete(task);
                }
            }
        }
    }
}
