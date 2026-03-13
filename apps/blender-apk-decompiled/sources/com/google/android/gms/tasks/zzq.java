package com.google.android.gms.tasks;

import java.util.concurrent.Callable;

/* JADX INFO: loaded from: classes.dex */
final class zzq implements Runnable {
    private /* synthetic */ Callable val$callable;
    private /* synthetic */ zzp zzler;

    zzq(zzp zzpVar, Callable callable) {
        this.zzler = zzpVar;
        this.val$callable = callable;
    }

    @Override // java.lang.Runnable
    public final void run() {
        try {
            this.zzler.setResult(this.val$callable.call());
        } catch (Exception e) {
            this.zzler.setException(e);
        }
    }
}
