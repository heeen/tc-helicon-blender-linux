package com.flurry.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;

/* JADX INFO: loaded from: classes.dex */
public class jr {
    private static final String e = "jr";
    private static jr f;
    public final Context a;
    public final Handler c;
    public final String d;
    private final kh h;
    public final Handler b = new Handler(Looper.getMainLooper());
    private final HandlerThread g = new HandlerThread("FlurryAgent");

    public static jr a() {
        return f;
    }

    public static synchronized void a(Context context, String str) {
        if (f != null) {
            if (!f.d.equals(str)) {
                throw new IllegalStateException("Only one API key per application is supported!");
            }
            kf.e(e, "Flurry is already initialized");
        } else {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
            if (TextUtils.isEmpty(str)) {
                throw new IllegalArgumentException("API key must be specified");
            }
            jr jrVar = new jr(context, str);
            f = jrVar;
            jrVar.h.a(context);
        }
    }

    public static synchronized void b() {
        if (f == null) {
            return;
        }
        jr jrVar = f;
        jrVar.h.a();
        jrVar.g.quit();
        f = null;
    }

    private jr(Context context, String str) {
        this.a = context.getApplicationContext();
        this.g.start();
        this.c = new Handler(this.g.getLooper());
        this.d = str;
        this.h = new kh();
    }

    public final void a(Runnable runnable) {
        this.b.post(runnable);
    }

    public final void b(Runnable runnable) {
        this.c.post(runnable);
    }

    public final void a(Runnable runnable, long j) {
        if (runnable == null) {
            return;
        }
        this.c.postDelayed(runnable, j);
    }

    public final void c(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        this.c.removeCallbacks(runnable);
    }

    public final ki a(Class<? extends ki> cls) {
        return this.h.b(cls);
    }
}
