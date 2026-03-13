package com.flurry.sdk;

import java.util.Timer;
import java.util.TimerTask;

/* JADX INFO: loaded from: classes.dex */
public class km {
    private static final String a = "km";
    private Timer b;
    private a c;
    private kn d;

    public km(kn knVar) {
        this.d = knVar;
    }

    public final synchronized void a() {
        if (this.b != null) {
            this.b.cancel();
            this.b = null;
            kf.a(3, a, "HttpRequestTimeoutTimer stopped.");
        }
        this.c = null;
    }

    class a extends TimerTask {
        private a() {
        }

        /* synthetic */ a(km kmVar, byte b) {
            this();
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public final void run() {
            kf.a(3, km.a, "HttpRequest timed out. Cancelling.");
            kn knVar = km.this.d;
            long jCurrentTimeMillis = System.currentTimeMillis() - knVar.m;
            kf.a(3, kn.e, "Timeout (" + jCurrentTimeMillis + "MS) for url: " + knVar.f);
            knVar.p = 629;
            knVar.t = true;
            knVar.h();
            knVar.f();
        }
    }

    public final synchronized void a(long j) {
        byte b = 0;
        if (this.b != null) {
            a();
        }
        this.b = new Timer("HttpRequestTimeoutTimer");
        this.c = new a(this, b);
        this.b.schedule(this.c, j);
        kf.a(3, a, "HttpRequestTimeoutTimer started: " + j + "MS");
    }
}
