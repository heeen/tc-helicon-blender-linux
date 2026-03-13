package com.flurry.sdk;

import java.util.Timer;
import java.util.TimerTask;

/* JADX INFO: loaded from: classes.dex */
final class lg {
    private Timer a;
    private a b;

    class a extends TimerTask {
        a() {
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public final void run() {
            lg.this.a();
            kb.a().a(new lh());
        }
    }

    lg() {
    }

    public final synchronized void a() {
        if (this.a != null) {
            this.a.cancel();
            this.a = null;
        }
        this.b = null;
    }

    public final synchronized void a(long j) {
        if (this.a != null) {
            a();
        }
        this.a = new Timer("FlurrySessionTimer");
        this.b = new a();
        this.a.schedule(this.b, j);
    }
}
