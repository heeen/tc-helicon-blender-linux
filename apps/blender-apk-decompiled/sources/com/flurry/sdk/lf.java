package com.flurry.sdk;

import android.app.Activity;
import android.content.Context;
import com.flurry.sdk.ju;
import com.flurry.sdk.le;
import com.flurry.sdk.lj;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes.dex */
public class lf implements lj.a {
    private static lf b = null;
    private static final String c = "lf";
    public long a;
    private long g;
    private ld h;
    private final Map<Context, ld> d = new WeakHashMap();
    private final lg e = new lg();
    private final Object f = new Object();
    private ka<lh> i = new ka<lh>() { // from class: com.flurry.sdk.lf.1
        @Override // com.flurry.sdk.ka
        public final /* bridge */ /* synthetic */ void a(jz jzVar) {
            lf.this.h();
        }
    };
    private ka<ju> j = new ka<ju>() { // from class: com.flurry.sdk.lf.2
        @Override // com.flurry.sdk.ka
        public final /* synthetic */ void a(jz jzVar) {
            ju juVar = (ju) jzVar;
            Activity activity = juVar.a.get();
            if (activity == null) {
                kf.a(lf.c, "Activity has been destroyed, can't pass ActivityLifecycleEvent to adobject.");
            }
            switch (AnonymousClass5.a[juVar.b.ordinal()]) {
                case 1:
                    kf.a(3, lf.c, "Automatic onStartSession for context:" + juVar.a);
                    lf.this.e(activity);
                    break;
                case 2:
                    kf.a(3, lf.c, "Automatic onEndSession for context:" + juVar.a);
                    lf.this.d(activity);
                    break;
                case 3:
                    kf.a(3, lf.c, "Automatic onEndSession (destroyed) for context:" + juVar.a);
                    lf.this.d(activity);
                    break;
            }
        }
    };

    public static synchronized lf a() {
        if (b == null) {
            b = new lf();
        }
        return b;
    }

    public static synchronized void b() {
        if (b != null) {
            kb.a().a(b.i);
            kb.a().a(b.j);
            li.a().b("ContinueSessionMillis", b);
        }
        b = null;
    }

    /* JADX INFO: renamed from: com.flurry.sdk.lf$5, reason: invalid class name */
    static /* synthetic */ class AnonymousClass5 {
        static final /* synthetic */ int[] a = new int[ju.a.values().length];

        static {
            try {
                a[ju.a.kStarted.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                a[ju.a.kStopped.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                a[ju.a.kDestroyed.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
        }
    }

    private lf() {
        li liVarA = li.a();
        this.a = 0L;
        this.g = ((Long) liVarA.a("ContinueSessionMillis")).longValue();
        liVarA.a("ContinueSessionMillis", (lj.a) this);
        kf.a(4, c, "initSettings, ContinueSessionMillis = " + this.g);
        kb.a().a("com.flurry.android.sdk.ActivityLifecycleEvent", this.j);
        kb.a().a("com.flurry.android.sdk.FlurrySessionTimerEvent", this.i);
    }

    private synchronized int g() {
        return this.d.size();
    }

    public final ld c() {
        ld ldVar;
        synchronized (this.f) {
            ldVar = this.h;
        }
        return ldVar;
    }

    public final synchronized void a(Context context) {
        if (context instanceof Activity) {
            if (jv.a().c()) {
                kf.a(3, c, "bootstrap for context:" + context);
                e(context);
            }
        }
    }

    public final synchronized void b(Context context) {
        if (jv.a().c() && (context instanceof Activity)) {
            return;
        }
        kf.a(3, c, "Manual onStartSession for context:" + context);
        e(context);
    }

    public final synchronized void c(Context context) {
        if (jv.a().c() && (context instanceof Activity)) {
            return;
        }
        kf.a(3, c, "Manual onEndSession for context:" + context);
        d(context);
    }

    public final synchronized boolean d() {
        if (c() != null) {
            return true;
        }
        kf.a(2, c, "Session not found. No active session");
        return false;
    }

    public final synchronized void e() {
        for (Map.Entry<Context, ld> entry : this.d.entrySet()) {
            le leVar = new le();
            leVar.a = new WeakReference<>(entry.getKey());
            leVar.b = entry.getValue();
            leVar.c = le.a.d;
            jd.a();
            leVar.d = jd.d();
            leVar.b();
        }
        this.d.clear();
        jr.a().b(new lw() { // from class: com.flurry.sdk.lf.3
            @Override // com.flurry.sdk.lw
            public final void a() {
                lf.this.h();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void e(Context context) {
        if (this.d.get(context) != null) {
            if (jv.a().c()) {
                kf.a(3, c, "Session already started with context:" + context);
                return;
            }
            kf.e(c, "Session already started with context:" + context);
            return;
        }
        this.e.a();
        ld ldVarC = c();
        if (ldVarC == null) {
            ldVarC = new ld();
            kf.e(c, "Flurry session started for context:" + context);
            le leVar = new le();
            leVar.a = new WeakReference<>(context);
            leVar.b = ldVarC;
            leVar.c = le.a.a;
            leVar.b();
        }
        this.d.put(context, ldVarC);
        synchronized (this.f) {
            this.h = ldVarC;
        }
        kf.e(c, "Flurry session resumed for context:" + context);
        le leVar2 = new le();
        leVar2.a = new WeakReference<>(context);
        leVar2.b = ldVarC;
        leVar2.c = le.a.c;
        leVar2.b();
        this.a = 0L;
    }

    final synchronized void d(Context context) {
        ld ldVarRemove = this.d.remove(context);
        if (ldVarRemove == null) {
            if (jv.a().c()) {
                kf.a(3, c, "Session cannot be ended, session not found for context:" + context);
                return;
            }
            kf.e(c, "Session cannot be ended, session not found for context:" + context);
            return;
        }
        kf.e(c, "Flurry session paused for context:" + context);
        le leVar = new le();
        leVar.a = new WeakReference<>(context);
        leVar.b = ldVarRemove;
        jd.a();
        leVar.d = jd.d();
        leVar.c = le.a.d;
        leVar.b();
        if (g() == 0) {
            this.e.a(this.g);
            this.a = System.currentTimeMillis();
        } else {
            this.a = 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void h() {
        int iG = g();
        if (iG > 0) {
            kf.a(5, c, "Session cannot be finalized, sessionContextCount:" + iG);
            return;
        }
        final ld ldVarC = c();
        if (ldVarC == null) {
            kf.a(5, c, "Session cannot be finalized, current session not found");
            return;
        }
        kf.e(c, "Flurry session ended");
        le leVar = new le();
        leVar.b = ldVarC;
        leVar.c = le.a.e;
        jd.a();
        leVar.d = jd.d();
        leVar.b();
        jr.a().b(new lw() { // from class: com.flurry.sdk.lf.4
            @Override // com.flurry.sdk.lw
            public final void a() {
                lf.a(lf.this, ldVarC);
            }
        });
    }

    @Override // com.flurry.sdk.lj.a
    public final void a(String str, Object obj) {
        if (str.equals("ContinueSessionMillis")) {
            this.g = ((Long) obj).longValue();
            kf.a(4, c, "onSettingUpdate, ContinueSessionMillis = " + this.g);
            return;
        }
        kf.a(6, c, "onSettingUpdate internal error!");
    }

    static /* synthetic */ void a(lf lfVar, ld ldVar) {
        synchronized (lfVar.f) {
            if (lfVar.h == ldVar) {
                lfVar.h = null;
            }
        }
    }
}
