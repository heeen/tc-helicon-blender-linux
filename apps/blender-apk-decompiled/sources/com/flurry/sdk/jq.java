package com.flurry.sdk;

import android.content.Context;
import android.os.SystemClock;
import com.flurry.sdk.le;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class jq {
    static final String a = "jq";
    WeakReference<ld> b;
    private String i;
    private String j;
    private Map<String, String> k;
    private final ka<le> g = new ka<le>() { // from class: com.flurry.sdk.jq.1
        @Override // com.flurry.sdk.ka
        public final /* synthetic */ void a(jz jzVar) {
            le leVar = (le) jzVar;
            if (jq.this.b == null || leVar.b == jq.this.b.get()) {
                switch (AnonymousClass4.a[leVar.c - 1]) {
                    case 1:
                        final jq jqVar = jq.this;
                        ld ldVar = leVar.b;
                        Context context = leVar.a.get();
                        jqVar.b = new WeakReference<>(ldVar);
                        jqVar.c = System.currentTimeMillis();
                        jqVar.d = SystemClock.elapsedRealtime();
                        if (ldVar == null || context == null) {
                            kf.a(3, jq.a, "Flurry session id cannot be created.");
                        } else {
                            kf.a(3, jq.a, "Flurry session id started:" + jqVar.c);
                            le leVar2 = new le();
                            leVar2.a = new WeakReference<>(context);
                            leVar2.b = ldVar;
                            leVar2.c = le.a.b;
                            leVar2.b();
                        }
                        jr.a().b(new lw() { // from class: com.flurry.sdk.jq.3
                            @Override // com.flurry.sdk.lw
                            public final void a() {
                                ji.a().c();
                            }
                        });
                        break;
                    case 2:
                        jq jqVar2 = jq.this;
                        leVar.a.get();
                        jqVar2.a();
                        break;
                    case 3:
                        jq jqVar3 = jq.this;
                        leVar.a.get();
                        jqVar3.e = SystemClock.elapsedRealtime() - jqVar3.d;
                        break;
                    case 4:
                        kb.a().b("com.flurry.android.sdk.FlurrySessionEvent", jq.this.g);
                        jq.b();
                        break;
                }
            }
        }
    };
    public volatile long c = 0;
    public volatile long d = 0;
    public volatile long e = -1;
    public volatile long f = 0;
    private volatile long h = 0;

    public static void b() {
    }

    /* JADX INFO: renamed from: com.flurry.sdk.jq$4, reason: invalid class name */
    static /* synthetic */ class AnonymousClass4 {
        static final /* synthetic */ int[] a = new int[le.a.a().length];

        static {
            try {
                a[le.a.a - 1] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                a[le.a.c - 1] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                a[le.a.d - 1] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                a[le.a.e - 1] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    public jq() {
        kb.a().a("com.flurry.android.sdk.FlurrySessionEvent", this.g);
        this.k = new LinkedHashMap<String, String>() { // from class: com.flurry.sdk.jq.2
            @Override // java.util.LinkedHashMap
            protected final boolean removeEldestEntry(Map.Entry<String, String> entry) {
                return size() > 10;
            }
        };
    }

    public final synchronized void a() {
        long j = lf.a().a;
        if (j > 0) {
            this.f += System.currentTimeMillis() - j;
        }
    }

    public final synchronized long c() {
        long jElapsedRealtime = SystemClock.elapsedRealtime() - this.d;
        if (jElapsedRealtime <= this.h) {
            jElapsedRealtime = this.h + 1;
            this.h = jElapsedRealtime;
        }
        this.h = jElapsedRealtime;
        return this.h;
    }

    public final synchronized void a(String str) {
        this.i = str;
    }

    public final synchronized void b(String str) {
        this.j = str;
    }

    public final synchronized String d() {
        return this.i;
    }

    public final synchronized String e() {
        return this.j;
    }

    public final synchronized void a(String str, String str2) {
        this.k.put(str, str2);
    }

    public final synchronized Map<String, String> f() {
        return this.k;
    }
}
