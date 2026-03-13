package com.flurry.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import com.flurry.android.FlurryEventRecordStatus;
import com.flurry.sdk.iy;
import com.flurry.sdk.le;
import com.flurry.sdk.lj;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: classes.dex */
public class ja implements lj.a {
    static final String a = "ja";
    static int b = 100;
    static int c = 10;
    static int d = 1000;
    static int e = 160000;
    static int f = 50;
    WeakReference<ld> g;
    File h;
    jy<List<iy>> i;
    public boolean j;
    boolean k;
    String l;
    byte m;
    Long n;
    private long u;
    private final AtomicInteger q = new AtomicInteger(0);
    private final AtomicInteger r = new AtomicInteger(0);
    private final AtomicInteger s = new AtomicInteger(0);
    private final ka<le> t = new ka<le>() { // from class: com.flurry.sdk.ja.1
        @Override // com.flurry.sdk.ka
        public final /* synthetic */ void a(jz jzVar) {
            le leVar = (le) jzVar;
            if (ja.this.g == null || leVar.b == ja.this.g.get()) {
                switch (AnonymousClass8.a[leVar.c - 1]) {
                    case 1:
                        final ja jaVar = ja.this;
                        ld ldVar = leVar.b;
                        Context context = leVar.a.get();
                        jaVar.g = new WeakReference<>(ldVar);
                        li liVarA = li.a();
                        jaVar.k = ((Boolean) liVarA.a("LogEvents")).booleanValue();
                        liVarA.a("LogEvents", (lj.a) jaVar);
                        kf.a(4, ja.a, "initSettings, LogEvents = " + jaVar.k);
                        jaVar.l = (String) liVarA.a("UserId");
                        liVarA.a("UserId", (lj.a) jaVar);
                        kf.a(4, ja.a, "initSettings, UserId = " + jaVar.l);
                        jaVar.m = ((Byte) liVarA.a("Gender")).byteValue();
                        liVarA.a("Gender", (lj.a) jaVar);
                        kf.a(4, ja.a, "initSettings, Gender = " + ((int) jaVar.m));
                        jaVar.n = (Long) liVarA.a("Age");
                        liVarA.a("Age", (lj.a) jaVar);
                        kf.a(4, ja.a, "initSettings, BirthDate = " + jaVar.n);
                        jaVar.o = ((Boolean) liVarA.a("analyticsEnabled")).booleanValue();
                        liVarA.a("analyticsEnabled", (lj.a) jaVar);
                        kf.a(4, ja.a, "initSettings, AnalyticsEnabled = " + jaVar.o);
                        jaVar.h = context.getFileStreamPath(".flurryagent." + Integer.toString(jr.a().d.hashCode(), 16));
                        jaVar.i = new jy<>(context.getFileStreamPath(".yflurryreport." + Long.toString(lr.i(jr.a().d), 16)), ".yflurryreport.", 1, new lc<List<iy>>() { // from class: com.flurry.sdk.ja.10
                            @Override // com.flurry.sdk.lc
                            public final kz<List<iy>> a(int i) {
                                return new ky(new iy.a());
                            }
                        });
                        jaVar.a(context);
                        jaVar.a(true);
                        if (hk.a().a != null) {
                            jr.a().b(new lw() { // from class: com.flurry.sdk.ja.11
                                @Override // com.flurry.sdk.lw
                                public final void a() {
                                    hk.a().a.a();
                                }
                            });
                        }
                        jr.a().b(new lw() { // from class: com.flurry.sdk.ja.12
                            @Override // com.flurry.sdk.lw
                            public final void a() {
                                ja.this.e();
                            }
                        });
                        jr.a().b(new lw() { // from class: com.flurry.sdk.ja.13
                            @Override // com.flurry.sdk.lw
                            public final void a() {
                                ja.d(ja.this);
                            }
                        });
                        if (je.a().c()) {
                            jr.a().b(new lw() { // from class: com.flurry.sdk.ja.14
                                @Override // com.flurry.sdk.lw
                                public final void a() {
                                    ja jaVar2 = ja.this;
                                    jd.a();
                                    jaVar2.a(true, jd.d());
                                }
                            });
                        } else {
                            kb.a().a("com.flurry.android.sdk.IdProviderFinishedEvent", jaVar.p);
                        }
                        break;
                    case 2:
                        ja jaVar2 = ja.this;
                        leVar.a.get();
                        jaVar2.a();
                        break;
                    case 3:
                        ja jaVar3 = ja.this;
                        leVar.a.get();
                        jaVar3.b();
                        break;
                    case 4:
                        kb.a().b("com.flurry.android.sdk.FlurrySessionEvent", ja.this.t);
                        ja.this.a(leVar.d);
                        break;
                }
            }
        }
    };
    private int v = -1;
    private final List<iy> w = new ArrayList();
    private final Map<String, List<String>> x = new HashMap();
    private final Map<String, String> y = new HashMap();
    private final Map<String, iu> z = new HashMap();
    private final List<iv> A = new ArrayList();
    private boolean B = true;
    private int C = 0;
    private final List<it> D = new ArrayList();
    private int E = 0;
    private int F = 0;
    boolean o = true;
    private final hl G = new hl();
    final ka<jf> p = new ka<jf>() { // from class: com.flurry.sdk.ja.9
        @Override // com.flurry.sdk.ka
        public final /* synthetic */ void a(jz jzVar) {
            jr.a().b(new lw() { // from class: com.flurry.sdk.ja.9.1
                @Override // com.flurry.sdk.lw
                public final void a() {
                    ja jaVar = ja.this;
                    jd.a();
                    jaVar.a(true, jd.d());
                }
            });
        }
    };

    /* JADX INFO: renamed from: com.flurry.sdk.ja$8, reason: invalid class name */
    static /* synthetic */ class AnonymousClass8 {
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

    public ja() {
        kb.a().a("com.flurry.android.sdk.FlurrySessionEvent", this.t);
    }

    public final synchronized void a() {
        this.v = lp.e();
        if (hk.a().c != null) {
            jr.a().b(new lw() { // from class: com.flurry.sdk.ja.15
                @Override // com.flurry.sdk.lw
                public final void a() {
                    hk.a().c.d();
                }
            });
        }
        if (this.o && hk.a().a != null) {
            jr.a().b(new lw() { // from class: com.flurry.sdk.ja.16
                @Override // com.flurry.sdk.lw
                public final void a() {
                    hk.a().a.b();
                }
            });
        }
    }

    public final synchronized void b() {
        a(false);
        jd.a();
        final long jD = jd.d();
        jd.a();
        final long jF = jd.f();
        jd.a();
        jq jqVarI = jd.i();
        final long j = jqVarI != null ? jqVarI.f : 0L;
        jd.a();
        final int i = jd.h().e;
        jd.a();
        b(jd.f());
        if (this.o && hk.a().a != null) {
            jr.a().b(new lw() { // from class: com.flurry.sdk.ja.2
                @Override // com.flurry.sdk.lw
                public final void a() {
                    hk.a().a.a(jD);
                }
            });
        }
        jr.a().b(new lw() { // from class: com.flurry.sdk.ja.3
            @Override // com.flurry.sdk.lw
            public final void a() {
                ja.this.f();
            }
        });
        if (je.a().c()) {
            jr.a().b(new lw() { // from class: com.flurry.sdk.ja.4
                @Override // com.flurry.sdk.lw
                public final void a() {
                    iy iyVarA = ja.this.a(jD, jF, j, i);
                    ja.this.w.clear();
                    ja.this.w.add(iyVarA);
                    ja.this.d();
                }
            });
        }
    }

    public final synchronized void a(final long j) {
        kb.a().a(this.p);
        jr.a().b(new lw() { // from class: com.flurry.sdk.ja.5
            @Override // com.flurry.sdk.lw
            public final void a() {
                if (ja.this.o && hk.a().a != null) {
                    hk.a().a.c();
                }
                if (hk.a().c != null) {
                    jr.a().b(new lw() { // from class: com.flurry.sdk.ja.5.1
                        @Override // com.flurry.sdk.lw
                        public final void a() {
                            hk.a().c.c = true;
                        }
                    });
                }
            }
        });
        if (je.a().c()) {
            jr.a().b(new lw() { // from class: com.flurry.sdk.ja.6
                @Override // com.flurry.sdk.lw
                public final void a() {
                    ja.this.a(false, j);
                }
            });
        }
        li.a().b("Gender", this);
        li.a().b("UserId", this);
        li.a().b("Age", this);
        li.a().b("LogEvents", this);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:20:0x003b  */
    @Override // com.flurry.sdk.lj.a
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final void a(java.lang.String r3, java.lang.Object r4) {
        /*
            Method dump skipped, instruction units count: 250
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.flurry.sdk.ja.a(java.lang.String, java.lang.Object):void");
    }

    final void a(Context context) {
        Bundle extras;
        if (!(context instanceof Activity) || (extras = ((Activity) context).getIntent().getExtras()) == null) {
            return;
        }
        kf.a(3, a, "Launch Options Bundle is present " + extras.toString());
        for (String str : extras.keySet()) {
            if (str != null) {
                Object obj = extras.get(str);
                String string = obj != null ? obj.toString() : "null";
                this.x.put(str, new ArrayList(Arrays.asList(string)));
                kf.a(3, a, "Launch options Key: " + str + ". Its value: " + string);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:38:0x0184  */
    /* JADX WARN: Removed duplicated region for block: B:39:0x0187  */
    /* JADX WARN: Removed duplicated region for block: B:42:0x01a2  */
    /* JADX WARN: Removed duplicated region for block: B:43:0x01a5  */
    @android.annotation.TargetApi(18)
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    final void a(boolean r9) {
        /*
            Method dump skipped, instruction units count: 438
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.flurry.sdk.ja.a(boolean):void");
    }

    private synchronized void b(long j) {
        for (iv ivVar : this.A) {
            if (ivVar.b && !ivVar.c) {
                ivVar.a(j);
            }
        }
    }

    final synchronized iy a(long j, long j2, long j3, int i) {
        iy iyVar;
        iz izVar = new iz();
        izVar.a = jn.a().i();
        izVar.b = j;
        izVar.c = j2;
        izVar.d = j3;
        izVar.e = this.y;
        jd.a();
        jq jqVarI = jd.i();
        izVar.f = jqVarI != null ? jqVarI.d() : null;
        jd.a();
        jq jqVarI2 = jd.i();
        izVar.g = jqVarI2 != null ? jqVarI2.e() : null;
        jd.a();
        jq jqVarI3 = jd.i();
        izVar.h = jqVarI3 != null ? jqVarI3.f() : null;
        jh.a();
        izVar.i = jh.b();
        jh.a();
        izVar.j = TimeZone.getDefault().getID();
        izVar.k = i;
        izVar.l = this.v != -1 ? this.v : lp.e();
        izVar.m = this.l == null ? "" : this.l;
        izVar.n = ji.a().e();
        izVar.o = this.F;
        izVar.p = this.m;
        izVar.q = this.n;
        izVar.r = this.z;
        izVar.s = this.A;
        izVar.t = this.B;
        izVar.v = this.D;
        izVar.u = this.E;
        try {
            iyVar = new iy(izVar);
        } catch (IOException e2) {
            kf.a(5, a, "Error creating analytics session report: " + e2);
            iyVar = null;
        }
        if (iyVar == null) {
            kf.e(a, "New session report wasn't created");
        }
        return iyVar;
    }

    public final synchronized void c() {
        this.F++;
    }

    public final synchronized FlurryEventRecordStatus a(String str, String str2, Map<String, String> map) {
        FlurryEventRecordStatus flurryEventRecordStatus = FlurryEventRecordStatus.kFlurryEventFailed;
        if (map == null) {
            return flurryEventRecordStatus;
        }
        if (TextUtils.isEmpty(str2)) {
            return flurryEventRecordStatus;
        }
        map.put("\ue8ffsid+Tumblr", str2);
        FlurryEventRecordStatus flurryEventRecordStatusA = a(str, map, false);
        kf.a(5, a, "logEvent status for syndication:" + flurryEventRecordStatusA);
        return flurryEventRecordStatusA;
    }

    public final synchronized FlurryEventRecordStatus a(String str, Map<String, String> map, boolean z) {
        FlurryEventRecordStatus flurryEventRecordStatus = FlurryEventRecordStatus.kFlurryEventRecorded;
        if (!this.o) {
            FlurryEventRecordStatus flurryEventRecordStatus2 = FlurryEventRecordStatus.kFlurryEventAnalyticsDisabled;
            kf.e(a, "Analytics has been disabled, not logging event.");
            return flurryEventRecordStatus2;
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        jd.a();
        long jE = jElapsedRealtime - jd.e();
        final String strB = lr.b(str);
        if (strB.length() == 0) {
            return FlurryEventRecordStatus.kFlurryEventFailed;
        }
        iu iuVar = this.z.get(strB);
        if (iuVar == null) {
            if (this.z.size() < b) {
                iu iuVar2 = new iu();
                iuVar2.a = 1;
                this.z.put(strB, iuVar2);
                kf.e(a, "Event count started: " + strB);
            } else {
                kf.e(a, "Too many different events. Event not counted: " + strB);
                flurryEventRecordStatus = FlurryEventRecordStatus.kFlurryEventUniqueCountExceeded;
            }
        } else {
            iuVar.a++;
            kf.e(a, "Event count incremented: " + strB);
            flurryEventRecordStatus = FlurryEventRecordStatus.kFlurryEventRecorded;
        }
        if (this.k && this.A.size() < d && this.C < e) {
            final Map<String, String> mapEmptyMap = map == null ? Collections.emptyMap() : map;
            if (mapEmptyMap.size() > c) {
                kf.e(a, "MaxEventParams exceeded: " + mapEmptyMap.size());
                flurryEventRecordStatus = FlurryEventRecordStatus.kFlurryEventParamsCountExceeded;
            } else {
                iv ivVar = new iv(this.q.incrementAndGet(), strB, mapEmptyMap, jE, z);
                if (ivVar.b().length + this.C <= e) {
                    this.A.add(ivVar);
                    this.C += ivVar.b().length;
                    FlurryEventRecordStatus flurryEventRecordStatus3 = FlurryEventRecordStatus.kFlurryEventRecorded;
                    if (this.o && hk.a().a != null) {
                        jr.a().b(new Runnable() { // from class: com.flurry.sdk.ja.7
                            @Override // java.lang.Runnable
                            public final void run() {
                                hk.a().a.a(strB, mapEmptyMap);
                            }
                        });
                    }
                    flurryEventRecordStatus = flurryEventRecordStatus3;
                } else {
                    this.C = e;
                    this.B = false;
                    kf.e(a, "Event Log size exceeded. No more event details logged.");
                    flurryEventRecordStatus = FlurryEventRecordStatus.kFlurryEventLogCountExceeded;
                }
            }
        } else {
            this.B = false;
        }
        return flurryEventRecordStatus;
    }

    public final synchronized void a(String str, Map<String, String> map) {
        for (iv ivVar : this.A) {
            if (ivVar.b && ivVar.d == 0 && ivVar.a.equals(str)) {
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                jd.a();
                long jE = jElapsedRealtime - jd.e();
                if (map != null && map.size() > 0 && this.C < e) {
                    int length = this.C - ivVar.b().length;
                    HashMap map2 = new HashMap(ivVar.a());
                    ivVar.a(map);
                    if (ivVar.b().length + length <= e) {
                        if (ivVar.a().size() > c) {
                            kf.e(a, "MaxEventParams exceeded on endEvent: " + ivVar.a().size());
                            ivVar.b(map2);
                        } else {
                            this.C = length + ivVar.b().length;
                        }
                    } else {
                        ivVar.b(map2);
                        this.B = false;
                        this.C = e;
                        kf.e(a, "Event Log size exceeded. No more event details logged.");
                    }
                }
                ivVar.a(jE);
                return;
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x0012  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final synchronized void a(java.lang.String r12, java.lang.String r13, java.lang.String r14, java.lang.Throwable r15) {
        /*
            r11 = this;
            monitor-enter(r11)
            r0 = 0
            r1 = 1
            if (r12 == 0) goto L12
            java.lang.String r2 = "uncaught"
            boolean r2 = r2.equals(r12)     // Catch: java.lang.Throwable -> Lf
            if (r2 == 0) goto L12
            r2 = r1
            goto L13
        Lf:
            r12 = move-exception
            goto Lab
        L12:
            r2 = r0
        L13:
            int r3 = r11.E     // Catch: java.lang.Throwable -> Lf
            int r3 = r3 + r1
            r11.E = r3     // Catch: java.lang.Throwable -> Lf
            java.util.List<com.flurry.sdk.it> r1 = r11.D     // Catch: java.lang.Throwable -> Lf
            int r1 = r1.size()     // Catch: java.lang.Throwable -> Lf
            int r3 = com.flurry.sdk.ja.f     // Catch: java.lang.Throwable -> Lf
            if (r1 >= r3) goto L5a
            long r0 = java.lang.System.currentTimeMillis()     // Catch: java.lang.Throwable -> Lf
            java.lang.Long r0 = java.lang.Long.valueOf(r0)     // Catch: java.lang.Throwable -> Lf
            com.flurry.sdk.it r9 = new com.flurry.sdk.it     // Catch: java.lang.Throwable -> Lf
            java.util.concurrent.atomic.AtomicInteger r1 = r11.r     // Catch: java.lang.Throwable -> Lf
            int r2 = r1.incrementAndGet()     // Catch: java.lang.Throwable -> Lf
            long r3 = r0.longValue()     // Catch: java.lang.Throwable -> Lf
            r1 = r9
            r5 = r12
            r6 = r13
            r7 = r14
            r8 = r15
            r1.<init>(r2, r3, r5, r6, r7, r8)     // Catch: java.lang.Throwable -> Lf
            java.util.List<com.flurry.sdk.it> r12 = r11.D     // Catch: java.lang.Throwable -> Lf
            r12.add(r9)     // Catch: java.lang.Throwable -> Lf
            java.lang.String r12 = com.flurry.sdk.ja.a     // Catch: java.lang.Throwable -> Lf
            java.lang.StringBuilder r13 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lf
            java.lang.String r14 = "Error logged: "
            r13.<init>(r14)     // Catch: java.lang.Throwable -> Lf
            java.lang.String r14 = r9.a     // Catch: java.lang.Throwable -> Lf
            r13.append(r14)     // Catch: java.lang.Throwable -> Lf
            java.lang.String r13 = r13.toString()     // Catch: java.lang.Throwable -> Lf
            com.flurry.sdk.kf.e(r12, r13)     // Catch: java.lang.Throwable -> Lf
            monitor-exit(r11)
            return
        L5a:
            if (r2 == 0) goto La2
        L5c:
            java.util.List<com.flurry.sdk.it> r1 = r11.D     // Catch: java.lang.Throwable -> Lf
            int r1 = r1.size()     // Catch: java.lang.Throwable -> Lf
            if (r0 >= r1) goto La0
            java.util.List<com.flurry.sdk.it> r1 = r11.D     // Catch: java.lang.Throwable -> Lf
            java.lang.Object r1 = r1.get(r0)     // Catch: java.lang.Throwable -> Lf
            com.flurry.sdk.it r1 = (com.flurry.sdk.it) r1     // Catch: java.lang.Throwable -> Lf
            java.lang.String r2 = r1.a     // Catch: java.lang.Throwable -> Lf
            if (r2 == 0) goto L9d
            java.lang.String r2 = "uncaught"
            java.lang.String r1 = r1.a     // Catch: java.lang.Throwable -> Lf
            boolean r1 = r2.equals(r1)     // Catch: java.lang.Throwable -> Lf
            if (r1 != 0) goto L9d
            long r1 = java.lang.System.currentTimeMillis()     // Catch: java.lang.Throwable -> Lf
            java.lang.Long r1 = java.lang.Long.valueOf(r1)     // Catch: java.lang.Throwable -> Lf
            com.flurry.sdk.it r10 = new com.flurry.sdk.it     // Catch: java.lang.Throwable -> Lf
            java.util.concurrent.atomic.AtomicInteger r2 = r11.r     // Catch: java.lang.Throwable -> Lf
            int r3 = r2.incrementAndGet()     // Catch: java.lang.Throwable -> Lf
            long r4 = r1.longValue()     // Catch: java.lang.Throwable -> Lf
            r2 = r10
            r6 = r12
            r7 = r13
            r8 = r14
            r9 = r15
            r2.<init>(r3, r4, r6, r7, r8, r9)     // Catch: java.lang.Throwable -> Lf
            java.util.List<com.flurry.sdk.it> r12 = r11.D     // Catch: java.lang.Throwable -> Lf
            r12.set(r0, r10)     // Catch: java.lang.Throwable -> Lf
            monitor-exit(r11)
            return
        L9d:
            int r0 = r0 + 1
            goto L5c
        La0:
            monitor-exit(r11)
            return
        La2:
            java.lang.String r12 = com.flurry.sdk.ja.a     // Catch: java.lang.Throwable -> Lf
            java.lang.String r13 = "Max errors logged. No more errors logged."
            com.flurry.sdk.kf.e(r12, r13)     // Catch: java.lang.Throwable -> Lf
            monitor-exit(r11)
            return
        Lab:
            monitor-exit(r11)
            throw r12
        */
        throw new UnsupportedOperationException("Method not decompiled: com.flurry.sdk.ja.a(java.lang.String, java.lang.String, java.lang.String, java.lang.Throwable):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void a(boolean z, long j) {
        byte[] bArr;
        if (!this.o) {
            kf.a(3, a, "Analytics disabled, not sending agent report.");
            return;
        }
        if (z || !this.w.isEmpty()) {
            kf.a(3, a, "generating agent report");
            try {
                bArr = new iw(jr.a().d, jn.a().i(), this.j, je.a().d(), this.u, j, this.w, Collections.unmodifiableMap(je.a().a), this.G.a(), this.x, jt.a().c(), System.currentTimeMillis()).a;
            } catch (Exception e2) {
                kf.e(a, "Exception while generating report: " + e2);
                bArr = null;
            }
            if (bArr == null) {
                kf.e(a, "Error generating report");
            } else {
                kf.a(3, a, "generated report of size " + bArr.length + " with " + this.w.size() + " reports.");
                ix ixVar = hk.a().b;
                StringBuilder sb = new StringBuilder();
                sb.append(js.a());
                ixVar.b(bArr, jr.a().d, sb.toString());
            }
            this.w.clear();
            this.i.b();
        }
    }

    public final synchronized void d() {
        kf.a(4, a, "Saving persistent agent data.");
        this.i.a(this.w);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void e() {
        kf.a(4, a, "Loading persistent session report data.");
        List<iy> listA = this.i.a();
        if (listA != null) {
            this.w.addAll(listA);
            return;
        }
        if (this.h.exists()) {
            kf.a(4, a, "Legacy persistent agent data found, converting.");
            jb jbVarA = hn.a(this.h);
            if (jbVarA != null) {
                boolean z = jbVarA.a;
                long jD = jbVarA.b;
                if (jD <= 0) {
                    jd.a();
                    jD = jd.d();
                }
                this.j = z;
                this.u = jD;
                f();
                List listUnmodifiableList = Collections.unmodifiableList(jbVarA.c);
                if (listUnmodifiableList != null) {
                    this.w.addAll(listUnmodifiableList);
                }
            }
            this.h.delete();
            d();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void f() {
        SharedPreferences.Editor editorEdit = jr.a().a.getSharedPreferences("FLURRY_SHARED_PREFERENCES", 0).edit();
        editorEdit.putBoolean("com.flurry.sdk.previous_successful_report", this.j);
        editorEdit.putLong("com.flurry.sdk.initial_run_time", this.u);
        editorEdit.commit();
    }

    static /* synthetic */ void d(ja jaVar) {
        SharedPreferences sharedPreferences = jr.a().a.getSharedPreferences("FLURRY_SHARED_PREFERENCES", 0);
        jaVar.j = sharedPreferences.getBoolean("com.flurry.sdk.previous_successful_report", false);
        jd.a();
        jaVar.u = sharedPreferences.getLong("com.flurry.sdk.initial_run_time", jd.d());
    }
}
