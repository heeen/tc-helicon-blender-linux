package com.flurry.sdk;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.text.TextUtils;
import com.flurry.sdk.id;
import com.flurry.sdk.im;
import com.flurry.sdk.kl;
import com.flurry.sdk.kn;
import com.flurry.sdk.lj;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: renamed from: com.flurry.sdk.if, reason: invalid class name */
/* JADX INFO: loaded from: classes.dex */
public class Cif implements lj.a {
    private static final String e = "if";
    private static String f = "https://proton.flurry.com/sdk/v1/config";
    private jy<id> i;
    private jy<List<im>> j;
    private boolean n;
    private String o;
    private boolean p;
    private boolean q;
    private long s;
    private boolean t;
    private hs u;
    private boolean v;
    public final Runnable a = new lw() { // from class: com.flurry.sdk.if.1
        @Override // com.flurry.sdk.lw
        public final void a() {
            Cif.this.f();
        }
    };
    public final ka<jf> b = new ka<jf>() { // from class: com.flurry.sdk.if.4
        @Override // com.flurry.sdk.ka
        public final /* bridge */ /* synthetic */ void a(jz jzVar) {
            Cif.this.f();
        }
    };
    public final ka<jg> c = new ka<jg>() { // from class: com.flurry.sdk.if.5
        @Override // com.flurry.sdk.ka
        public final /* bridge */ /* synthetic */ void a(jz jzVar) {
            Cif.this.f();
        }
    };
    public final ka<jj> d = new ka<jj>() { // from class: com.flurry.sdk.if.6
        @Override // com.flurry.sdk.ka
        public final /* bridge */ /* synthetic */ void a(jz jzVar) {
            if (((jj) jzVar).a) {
                Cif.this.f();
            }
        }
    };
    private final kj<hr> g = new kj<>("proton config request", new ir());
    private final kj<hs> h = new kj<>("proton config response", new is());
    private final ie k = new ie();
    private final jw<String, hv> l = new jw<>();
    private final List<im> m = new ArrayList();
    private long r = 10000;

    static /* synthetic */ boolean h(Cif cif) {
        cif.v = true;
        return true;
    }

    public Cif() {
        this.p = true;
        li liVarA = li.a();
        this.n = ((Boolean) liVarA.a("ProtonEnabled")).booleanValue();
        liVarA.a("ProtonEnabled", (lj.a) this);
        kf.a(4, e, "initSettings, protonEnabled = " + this.n);
        this.o = (String) liVarA.a("ProtonConfigUrl");
        liVarA.a("ProtonConfigUrl", (lj.a) this);
        kf.a(4, e, "initSettings, protonConfigUrl = " + this.o);
        this.p = ((Boolean) liVarA.a("analyticsEnabled")).booleanValue();
        liVarA.a("analyticsEnabled", (lj.a) this);
        kf.a(4, e, "initSettings, AnalyticsEnabled = " + this.p);
        kb.a().a("com.flurry.android.sdk.IdProviderFinishedEvent", this.b);
        kb.a().a("com.flurry.android.sdk.IdProviderUpdatedAdvertisingId", this.c);
        kb.a().a("com.flurry.android.sdk.NetworkStateEvent", this.d);
        this.i = new jy<>(jr.a().a.getFileStreamPath(".yflurryprotonconfig." + Long.toString(lr.i(jr.a().d), 16)), ".yflurryprotonconfig.", 1, new lc<id>() { // from class: com.flurry.sdk.if.7
            @Override // com.flurry.sdk.lc
            public final kz<id> a(int i) {
                return new id.a();
            }
        });
        this.j = new jy<>(jr.a().a.getFileStreamPath(".yflurryprotonreport." + Long.toString(lr.i(jr.a().d), 16)), ".yflurryprotonreport.", 1, new lc<List<im>>() { // from class: com.flurry.sdk.if.8
            @Override // com.flurry.sdk.lc
            public final kz<List<im>> a(int i) {
                return new ky(new im.a());
            }
        });
        jr.a().b(new lw() { // from class: com.flurry.sdk.if.9
            @Override // com.flurry.sdk.lw
            public final void a() {
                Cif.this.i();
            }
        });
        jr.a().b(new lw() { // from class: com.flurry.sdk.if.10
            @Override // com.flurry.sdk.lw
            public final void a() {
                Cif.this.k();
            }
        });
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x0032  */
    @Override // com.flurry.sdk.lj.a
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final void a(java.lang.String r3, java.lang.Object r4) {
        /*
            r2 = this;
            int r0 = r3.hashCode()
            r1 = -1720015653(0xffffffff997aa4db, float:-1.2957989E-23)
            if (r0 == r1) goto L28
            r1 = 640941243(0x2633fcbb, float:6.2445614E-16)
            if (r0 == r1) goto L1e
            r1 = 1591403975(0x5edae5c7, float:7.886616E18)
            if (r0 == r1) goto L14
            goto L32
        L14:
            java.lang.String r0 = "ProtonConfigUrl"
            boolean r3 = r3.equals(r0)
            if (r3 == 0) goto L32
            r3 = 1
            goto L33
        L1e:
            java.lang.String r0 = "ProtonEnabled"
            boolean r3 = r3.equals(r0)
            if (r3 == 0) goto L32
            r3 = 0
            goto L33
        L28:
            java.lang.String r0 = "analyticsEnabled"
            boolean r3 = r3.equals(r0)
            if (r3 == 0) goto L32
            r3 = 2
            goto L33
        L32:
            r3 = -1
        L33:
            r0 = 4
            switch(r3) {
                case 0: goto L78;
                case 1: goto L5e;
                case 2: goto L40;
                default: goto L37;
            }
        L37:
            r2 = 6
            java.lang.String r3 = com.flurry.sdk.Cif.e
            java.lang.String r4 = "onSettingUpdate internal error!"
            com.flurry.sdk.kf.a(r2, r3, r4)
            return
        L40:
            java.lang.Boolean r4 = (java.lang.Boolean) r4
            boolean r3 = r4.booleanValue()
            r2.p = r3
            java.lang.String r3 = com.flurry.sdk.Cif.e
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            java.lang.String r1 = "onSettingUpdate, AnalyticsEnabled = "
            r4.<init>(r1)
            boolean r2 = r2.p
            r4.append(r2)
            java.lang.String r2 = r4.toString()
            com.flurry.sdk.kf.a(r0, r3, r2)
            return
        L5e:
            java.lang.String r4 = (java.lang.String) r4
            r2.o = r4
            java.lang.String r3 = com.flurry.sdk.Cif.e
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            java.lang.String r1 = "onSettingUpdate, protonConfigUrl = "
            r4.<init>(r1)
            java.lang.String r2 = r2.o
            r4.append(r2)
            java.lang.String r2 = r4.toString()
            com.flurry.sdk.kf.a(r0, r3, r2)
            return
        L78:
            java.lang.Boolean r4 = (java.lang.Boolean) r4
            boolean r3 = r4.booleanValue()
            r2.n = r3
            java.lang.String r3 = com.flurry.sdk.Cif.e
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            java.lang.String r1 = "onSettingUpdate, protonEnabled = "
            r4.<init>(r1)
            boolean r2 = r2.n
            r4.append(r2)
            java.lang.String r2 = r4.toString()
            com.flurry.sdk.kf.a(r0, r3, r2)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.flurry.sdk.Cif.a(java.lang.String, java.lang.Object):void");
    }

    public final synchronized void a() {
        if (this.n) {
            lr.b();
            jd.a();
            ih.a = jd.d();
            this.v = false;
            f();
        }
    }

    public final synchronized void b() {
        if (this.n) {
            lr.b();
            jd.a();
            b(jd.d());
            j();
        }
    }

    public final synchronized void a(long j) {
        if (this.n) {
            lr.b();
            b(j);
            b("flurry.session_end", (Map<String, String>) null);
            jr.a().b(new lw() { // from class: com.flurry.sdk.if.11
                @Override // com.flurry.sdk.lw
                public final void a() {
                    Cif.this.l();
                }
            });
        }
    }

    public final synchronized void c() {
        if (this.n) {
            lr.b();
            j();
        }
    }

    public final synchronized void a(String str, Map<String, String> map) {
        if (this.n) {
            lr.b();
            b(str, map);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void e() {
        if (this.n) {
            lr.b();
            SharedPreferences sharedPreferences = jr.a().a.getSharedPreferences("FLURRY_SHARED_PREFERENCES", 0);
            if (sharedPreferences.getBoolean("com.flurry.android.flurryAppInstall", true)) {
                b("flurry.app_install", (Map<String, String>) null);
                SharedPreferences.Editor editorEdit = sharedPreferences.edit();
                editorEdit.putBoolean("com.flurry.android.flurryAppInstall", false);
                editorEdit.apply();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Type inference failed for: r3v3, types: [RequestObjectType, byte[]] */
    public synchronized void f() {
        if (this.n) {
            lr.b();
            if (this.q) {
                if (je.a().c()) {
                    final long jCurrentTimeMillis = System.currentTimeMillis();
                    final boolean z = !je.a().d();
                    if (this.u != null) {
                        if (this.t != z) {
                            kf.a(3, e, "Limit ad tracking value has changed, purging");
                            this.u = null;
                        } else {
                            if (System.currentTimeMillis() < this.s + (this.u.b * 1000)) {
                                kf.a(3, e, "Cached Proton config valid, no need to refresh");
                                if (!this.v) {
                                    this.v = true;
                                    b("flurry.session_start", (Map<String, String>) null);
                                }
                                return;
                            }
                            if (System.currentTimeMillis() >= this.s + (this.u.c * 1000)) {
                                kf.a(3, e, "Cached Proton config expired, purging");
                                this.u = null;
                                this.l.a();
                            }
                        }
                    }
                    jp.a().a(this);
                    kf.a(3, e, "Requesting proton config");
                    ?? G = g();
                    if (G == 0) {
                        return;
                    }
                    kl klVar = new kl();
                    klVar.f = TextUtils.isEmpty(this.o) ? f : this.o;
                    klVar.w = 5000;
                    klVar.g = kn.a.kPost;
                    klVar.a("Content-Type", "application/x-flurry;version=2");
                    klVar.a("Accept", "application/x-flurry;version=2");
                    klVar.a("FM-Checksum", Integer.toString(kj.a((byte[]) G)));
                    klVar.c = new kv();
                    klVar.d = new kv();
                    klVar.b = G;
                    klVar.a = new kl.a<byte[], byte[]>() { // from class: com.flurry.sdk.if.2
                        @Override // com.flurry.sdk.kl.a
                        public final /* synthetic */ void a(kl<byte[], byte[]> klVar2, byte[] bArr) {
                            hs hsVar;
                            final byte[] bArr2 = bArr;
                            int i = klVar2.p;
                            kf.a(3, Cif.e, "Proton config request: HTTP status code is:" + i);
                            if (i == 400 || i == 406 || i == 412 || i == 415) {
                                Cif.this.r = 10000L;
                                return;
                            }
                            if (klVar2.c() && bArr2 != null) {
                                jr.a().b(new lw() { // from class: com.flurry.sdk.if.2.1
                                    @Override // com.flurry.sdk.lw
                                    public final void a() {
                                        Cif.this.a(jCurrentTimeMillis, z, bArr2);
                                    }
                                });
                                try {
                                    hsVar = (hs) Cif.this.h.b(bArr2);
                                } catch (Exception e2) {
                                    kf.a(5, Cif.e, "Failed to decode proton config response: " + e2);
                                    hsVar = null;
                                }
                                hsVar = Cif.b(hsVar) ? hsVar : null;
                                if (hsVar != null) {
                                    Cif.this.r = 10000L;
                                    Cif.this.s = jCurrentTimeMillis;
                                    Cif.this.t = z;
                                    Cif.this.u = hsVar;
                                    Cif.this.h();
                                    if (!Cif.this.v) {
                                        Cif.h(Cif.this);
                                        Cif.this.b("flurry.session_start", (Map<String, String>) null);
                                    }
                                    Cif.this.e();
                                }
                            }
                            if (hsVar == null) {
                                long j = Cif.this.r << 1;
                                if (i == 429) {
                                    List<String> listA = klVar2.a("Retry-After");
                                    if (!listA.isEmpty()) {
                                        String str = listA.get(0);
                                        kf.a(3, Cif.e, "Server returned retry time: " + str);
                                        try {
                                            j = Long.parseLong(str) * 1000;
                                        } catch (NumberFormatException unused) {
                                            kf.a(3, Cif.e, "Server returned nonsensical retry time");
                                        }
                                    }
                                }
                                Cif.this.r = j;
                                kf.a(3, Cif.e, "Proton config request failed, backing off: " + Cif.this.r + "ms");
                                jr.a().a(Cif.this.a, Cif.this.r);
                            }
                        }
                    };
                    jp.a().a(this, klVar);
                }
            }
        }
    }

    private byte[] g() {
        try {
            hr hrVar = new hr();
            hrVar.a = jr.a().d;
            hrVar.b = lo.a(jr.a().a);
            hrVar.c = lo.b(jr.a().a);
            hrVar.d = js.a();
            hrVar.e = 3;
            jn.a();
            hrVar.f = jn.c();
            hrVar.g = !je.a().d();
            hrVar.h = new hu();
            hrVar.h.a = new ho();
            hrVar.h.a.a = Build.MODEL;
            hrVar.h.a.b = Build.BRAND;
            hrVar.h.a.c = Build.ID;
            hrVar.h.a.d = Build.DEVICE;
            hrVar.h.a.e = Build.PRODUCT;
            hrVar.h.a.f = Build.VERSION.RELEASE;
            hrVar.i = new ArrayList();
            for (Map.Entry entry : Collections.unmodifiableMap(je.a().a).entrySet()) {
                ht htVar = new ht();
                htVar.a = ((jm) entry.getKey()).c;
                if (((jm) entry.getKey()).d) {
                    htVar.b = new String((byte[]) entry.getValue());
                } else {
                    htVar.b = lr.b((byte[]) entry.getValue());
                }
                hrVar.i.add(htVar);
            }
            Location locationE = ji.a().e();
            if (locationE != null) {
                hrVar.j = new hy();
                hrVar.j.a = new hx();
                hrVar.j.a.a = lr.a(locationE.getLatitude());
                hrVar.j.a.b = lr.a(locationE.getLongitude());
                hrVar.j.a.c = (float) lr.a(locationE.getAccuracy());
            }
            String str = (String) li.a().a("UserId");
            if (!str.equals("")) {
                hrVar.k = new ib();
                hrVar.k.a = str;
            }
            return this.g.a(hrVar);
        } catch (Exception e2) {
            kf.a(5, e, "Proton config request failed with exception: " + e2);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:54:0x007f A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:56:0x0088 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static boolean b(com.flurry.sdk.hs r10) {
        /*
            r0 = 0
            if (r10 != 0) goto L4
            return r0
        L4:
            com.flurry.sdk.hq r1 = r10.e
            r2 = 3
            r3 = 1
            if (r1 == 0) goto L8b
            java.util.List<com.flurry.sdk.hp> r4 = r1.a
            if (r4 == 0) goto L8b
            r4 = r0
        Lf:
            java.util.List<com.flurry.sdk.hp> r5 = r1.a
            int r5 = r5.size()
            if (r4 >= r5) goto L8b
            java.util.List<com.flurry.sdk.hp> r5 = r1.a
            java.lang.Object r5 = r5.get(r4)
            com.flurry.sdk.hp r5 = (com.flurry.sdk.hp) r5
            if (r5 == 0) goto L88
            java.lang.String r6 = r5.b
            java.lang.String r7 = ""
            boolean r6 = r6.equals(r7)
            if (r6 != 0) goto L7f
            long r6 = r5.a
            r8 = -1
            int r6 = (r6 > r8 ? 1 : (r6 == r8 ? 0 : -1))
            if (r6 == 0) goto L7f
            java.lang.String r6 = r5.e
            java.lang.String r7 = ""
            boolean r6 = r6.equals(r7)
            if (r6 != 0) goto L7f
            java.util.List<com.flurry.sdk.hv> r5 = r5.c
            if (r5 == 0) goto L7c
            java.util.Iterator r5 = r5.iterator()
        L45:
            boolean r6 = r5.hasNext()
            if (r6 == 0) goto L7c
            java.lang.Object r6 = r5.next()
            com.flurry.sdk.hv r6 = (com.flurry.sdk.hv) r6
            java.lang.String r7 = r6.a
            java.lang.String r8 = ""
            boolean r7 = r7.equals(r8)
            if (r7 == 0) goto L64
            java.lang.String r5 = com.flurry.sdk.Cif.e
            java.lang.String r6 = "An event is missing a name"
            com.flurry.sdk.kf.a(r2, r5, r6)
        L62:
            r5 = r0
            goto L7d
        L64:
            boolean r7 = r6 instanceof com.flurry.sdk.hw
            if (r7 == 0) goto L45
            com.flurry.sdk.hw r6 = (com.flurry.sdk.hw) r6
            java.lang.String r6 = r6.c
            java.lang.String r7 = ""
            boolean r6 = r6.equals(r7)
            if (r6 == 0) goto L45
            java.lang.String r5 = com.flurry.sdk.Cif.e
            java.lang.String r6 = "An event trigger is missing a param name"
            com.flurry.sdk.kf.a(r2, r5, r6)
            goto L62
        L7c:
            r5 = r3
        L7d:
            if (r5 != 0) goto L88
        L7f:
            java.lang.String r1 = com.flurry.sdk.Cif.e
            java.lang.String r4 = "A callback template is missing required values"
            com.flurry.sdk.kf.a(r2, r1, r4)
            r1 = r0
            goto L8c
        L88:
            int r4 = r4 + 1
            goto Lf
        L8b:
            r1 = r3
        L8c:
            if (r1 == 0) goto La6
            com.flurry.sdk.hq r1 = r10.e
            if (r1 == 0) goto La5
            com.flurry.sdk.hq r1 = r10.e
            java.lang.String r1 = r1.e
            if (r1 == 0) goto La5
            com.flurry.sdk.hq r10 = r10.e
            java.lang.String r10 = r10.e
            java.lang.String r1 = ""
            boolean r10 = r10.equals(r1)
            if (r10 == 0) goto La5
            goto La6
        La5:
            return r3
        La6:
            java.lang.String r10 = com.flurry.sdk.Cif.e
            java.lang.String r1 = "Config response is missing required values."
            com.flurry.sdk.kf.a(r2, r10, r1)
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.flurry.sdk.Cif.b(com.flurry.sdk.hs):boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void h() {
        List<hp> list;
        List<hv> list2;
        if (this.u == null) {
            return;
        }
        kf.a(5, e, "Processing config response");
        il.a(this.u.e.c);
        il.b(this.u.e.d * 1000);
        in inVarA = in.a();
        String str = this.u.e.e;
        if (str != null && !str.endsWith(".do")) {
            kf.a(5, in.b, "overriding analytics agent report URL without an endpoint, are you sure?");
        }
        inVarA.a = str;
        if (this.n) {
            li.a().a("analyticsEnabled", Boolean.valueOf(this.u.f.b));
        }
        this.l.a();
        hq hqVar = this.u.e;
        if (hqVar == null || (list = hqVar.a) == null) {
            return;
        }
        for (hp hpVar : list) {
            if (hpVar != null && (list2 = hpVar.c) != null) {
                for (hv hvVar : list2) {
                    if (hvVar != null && !TextUtils.isEmpty(hvVar.a)) {
                        hvVar.b = hpVar;
                        this.l.a(hvVar.a, hvVar);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:105:0x0105 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:108:0x00fd A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public synchronized void b(java.lang.String r34, java.util.Map<java.lang.String, java.lang.String> r35) {
        /*
            Method dump skipped, instruction units count: 558
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.flurry.sdk.Cif.b(java.lang.String, java.util.Map):void");
    }

    private synchronized void b(long j) {
        Iterator<im> it = this.m.iterator();
        while (it.hasNext()) {
            if (j == it.next().a) {
                it.remove();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void i() {
        hs hsVarB;
        id idVarA = this.i.a();
        if (idVarA != null) {
            hs hsVar = null;
            try {
                hsVarB = this.h.b(idVarA.c);
            } catch (Exception e2) {
                kf.a(5, e, "Failed to decode saved proton config response: " + e2);
                this.i.b();
                hsVarB = null;
            }
            if (b(hsVarB)) {
                hsVar = hsVarB;
            }
            if (hsVar != null) {
                kf.a(4, e, "Loaded saved proton config response");
                this.r = 10000L;
                this.s = idVarA.a;
                this.t = idVarA.b;
                this.u = hsVar;
                h();
            }
            this.q = true;
            jr.a().b(new lw() { // from class: com.flurry.sdk.if.3
                @Override // com.flurry.sdk.lw
                public final void a() {
                    Cif.this.f();
                }
            });
        } else {
            this.q = true;
            jr.a().b(new lw() { // from class: com.flurry.sdk.if.3
                @Override // com.flurry.sdk.lw
                public final void a() {
                    Cif.this.f();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void a(long j, boolean z, byte[] bArr) {
        if (bArr == null) {
            return;
        }
        kf.a(4, e, "Saving proton config response");
        id idVar = new id();
        idVar.a = j;
        idVar.b = z;
        idVar.c = bArr;
        this.i.a(idVar);
    }

    private synchronized void j() {
        if (!this.p) {
            kf.e(e, "Analytics disabled, not sending pulse reports.");
            return;
        }
        kf.a(4, e, "Sending " + this.m.size() + " queued reports.");
        for (im imVar : this.m) {
            kf.a(3, e, "Firing Pulse callbacks for event: " + imVar.c);
            il.a().a(imVar);
        }
        m();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void k() {
        kf.a(4, e, "Loading queued report data.");
        List<im> listA = this.j.a();
        if (listA != null) {
            this.m.addAll(listA);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void l() {
        kf.a(4, e, "Saving queued report data.");
        this.j.a(this.m);
    }

    private synchronized void m() {
        this.m.clear();
        this.j.b();
    }
}
