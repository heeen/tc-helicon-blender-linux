package com.flurry.sdk;

import android.support.v7.widget.ActivityChooserView;
import com.flurry.sdk.kp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public abstract class kq<ReportInfo extends kp> {
    private static final String a = "kq";
    public static long b = 10000;
    public boolean c;
    public long d;
    private final jy<List<ReportInfo>> f;
    private int h;
    private final int e = ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
    private final List<ReportInfo> g = new ArrayList();
    private final Runnable i = new lw() { // from class: com.flurry.sdk.kq.1
        @Override // com.flurry.sdk.lw
        public final void a() {
            kq.this.b();
        }
    };
    private final ka<jj> j = new ka<jj>() { // from class: com.flurry.sdk.kq.2
        @Override // com.flurry.sdk.ka
        public final /* bridge */ /* synthetic */ void a(jz jzVar) {
            if (((jj) jzVar).a) {
                kq.this.b();
            }
        }
    };

    public abstract jy<List<ReportInfo>> a();

    public abstract void a(ReportInfo reportinfo);

    public kq() {
        kb.a().a("com.flurry.android.sdk.NetworkStateEvent", this.j);
        this.f = a();
        this.d = b;
        this.h = -1;
        jr.a().b(new lw() { // from class: com.flurry.sdk.kq.3
            @Override // com.flurry.sdk.lw
            public final void a() {
                kq.this.b(kq.this.g);
                kq.this.b();
            }
        });
    }

    public final void c() {
        jr.a().c(this.i);
        kb.a().b("com.flurry.android.sdk.NetworkStateEvent", this.j);
    }

    public final void d() {
        this.c = false;
        jr.a().b(new lw() { // from class: com.flurry.sdk.kq.4
            @Override // com.flurry.sdk.lw
            public final void a() {
                kq.this.b();
            }
        });
    }

    public final synchronized void b(ReportInfo reportinfo) {
        if (reportinfo == null) {
            return;
        }
        this.g.add(reportinfo);
        jr.a().b(new lw() { // from class: com.flurry.sdk.kq.5
            @Override // com.flurry.sdk.lw
            public final void a() {
                kq.this.b();
            }
        });
    }

    public final synchronized void d(ReportInfo reportinfo) {
        reportinfo.a_();
        jr.a().b(new lw() { // from class: com.flurry.sdk.kq.7
            @Override // com.flurry.sdk.lw
            public final void a() {
                kq.this.e();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void b() {
        if (this.c) {
            return;
        }
        if (this.h >= 0) {
            kf.a(3, a, "Transmit is in progress");
            return;
        }
        g();
        if (this.g.isEmpty()) {
            this.d = b;
            this.h = -1;
        } else {
            this.h = 0;
            jr.a().b(new lw() { // from class: com.flurry.sdk.kq.8
                @Override // com.flurry.sdk.lw
                public final void a() {
                    kq.this.e();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void e() {
        lr.b();
        ReportInfo reportinfo = null;
        if (jk.a().b) {
            while (true) {
                if (this.h >= this.g.size()) {
                    break;
                }
                List<ReportInfo> list = this.g;
                int i = this.h;
                this.h = i + 1;
                ReportInfo reportinfo2 = list.get(i);
                if (!reportinfo2.o) {
                    reportinfo = reportinfo2;
                    break;
                }
            }
        } else {
            kf.a(3, a, "Network is not available, aborting transmission");
        }
        if (reportinfo == null) {
            f();
        } else {
            a(reportinfo);
        }
    }

    private synchronized void f() {
        g();
        a(this.g);
        if (this.c) {
            kf.a(3, a, "Reporter paused");
            this.d = b;
        } else if (this.g.isEmpty()) {
            kf.a(3, a, "All reports sent successfully");
            this.d = b;
        } else {
            this.d <<= 1;
            kf.a(3, a, "One or more reports failed to send, backing off: " + this.d + "ms");
            jr.a().a(this.i, this.d);
        }
        this.h = -1;
    }

    public synchronized void b(List<ReportInfo> list) {
        lr.b();
        List<ReportInfo> listA = this.f.a();
        if (listA != null) {
            list.addAll(listA);
        }
    }

    public synchronized void a(List<ReportInfo> list) {
        lr.b();
        this.f.a(new ArrayList(list));
    }

    private synchronized void g() {
        Iterator<ReportInfo> it = this.g.iterator();
        while (it.hasNext()) {
            ReportInfo next = it.next();
            if (next.o) {
                kf.a(3, a, "Url transmitted - " + next.q + " Attempts: " + next.p);
                it.remove();
            } else if (next.p > next.a()) {
                kf.a(3, a, "Exceeded max no of attempts - " + next.q + " Attempts: " + next.p);
                it.remove();
            } else if (System.currentTimeMillis() > next.n && next.p > 0) {
                kf.a(3, a, "Expired: Time expired - " + next.q + " Attempts: " + next.p);
                it.remove();
            }
        }
    }

    public final synchronized void c(ReportInfo reportinfo) {
        reportinfo.o = true;
        jr.a().b(new lw() { // from class: com.flurry.sdk.kq.6
            @Override // com.flurry.sdk.lw
            public final void a() {
                kq.this.e();
            }
        });
    }
}
