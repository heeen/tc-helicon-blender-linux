package com.flurry.sdk;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Toast;
import com.flurry.sdk.im;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: classes.dex */
public class il {
    public static int a;
    public static int b;
    public static AtomicInteger c;
    static jy<List<im>> d;
    private static il f;
    private static Map<Integer, im> g;
    private final AtomicInteger h;
    private long i;
    private String e = il.class.getSimpleName();
    private ka<jj> j = new ka<jj>() { // from class: com.flurry.sdk.il.1
        @Override // com.flurry.sdk.ka
        public final /* synthetic */ void a(jz jzVar) {
            jj jjVar = (jj) jzVar;
            kf.a(4, il.this.e, "onNetworkStateChanged : isNetworkEnable = " + jjVar.a);
            if (jjVar.a) {
                jr.a().b(new Runnable() { // from class: com.flurry.sdk.il.1.1
                    @Override // java.lang.Runnable
                    public final void run() {
                        in.a().b();
                    }
                });
            }
        }
    };

    private il() {
        g = new HashMap();
        this.h = new AtomicInteger(0);
        c = new AtomicInteger(0);
        if (b == 0) {
            b = 600000;
        }
        if (a == 0) {
            a = 15;
        }
        this.i = jr.a().a.getSharedPreferences("FLURRY_SHARED_PREFERENCES", 0).getLong("timeToSendNextPulseReport", 0L);
        if (d == null) {
            m();
        }
        kb.a().a("com.flurry.android.sdk.NetworkStateEvent", this.j);
    }

    public static synchronized il a() {
        if (f == null) {
            f = new il();
        }
        return f;
    }

    public static void b() {
        if (f != null) {
            kb.a().b("com.flurry.android.sdk.NetworkStateEvent", f.j);
            g.clear();
            g = null;
            f = null;
        }
    }

    public static void a(int i) {
        a = i;
    }

    public static void b(int i) {
        b = i;
    }

    public final synchronized void a(im imVar) {
        if (imVar == null) {
            kf.a(3, this.e, "Must add valid PulseCallbackAsyncReportInfo");
            return;
        }
        kf.a(3, this.e, "Adding and sending " + imVar.c + " report to PulseCallbackManager.");
        if (imVar.a().size() != 0) {
            if (this.i == 0) {
                this.i = System.currentTimeMillis() + ((long) b);
                jr.a().b(new Runnable() { // from class: com.flurry.sdk.il.2
                    @Override // java.lang.Runnable
                    public final void run() {
                        il.this.k();
                    }
                });
            }
            int iL = l();
            imVar.b = iL;
            g.put(Integer.valueOf(iL), imVar);
            Iterator<ii> it = imVar.a().iterator();
            while (it.hasNext()) {
                hk.a().c.b(it.next());
            }
        }
    }

    public final synchronized void b(im imVar) {
        if (imVar == null) {
            kf.a(3, this.e, "Must add valid PulseCallbackAsyncReportInfo");
            return;
        }
        if (this.i == 0) {
            this.i = System.currentTimeMillis() + ((long) b);
            jr.a().b(new Runnable() { // from class: com.flurry.sdk.il.3
                @Override // java.lang.Runnable
                public final void run() {
                    il.this.k();
                }
            });
        }
        int iL = l();
        imVar.b = iL;
        g.put(Integer.valueOf(iL), imVar);
        Iterator<ii> it = imVar.a().iterator();
        while (it.hasNext()) {
            Iterator<ij> it2 = it.next().a.iterator();
            while (it2.hasNext()) {
                it2.next();
                c.incrementAndGet();
                if (h()) {
                    kf.a(3, this.e, "Max Callback Attempts threshold reached. Sending callback logging reports");
                    j();
                }
            }
        }
        if (i()) {
            kf.a(3, this.e, "Time threshold reached. Sending callback logging reports");
            j();
        }
        kf.a(3, this.e, "Restoring " + imVar.c + " report to PulseCallbackManager. Number of stored completed callbacks: " + c.get());
    }

    private synchronized void c(int i) {
        kf.a(3, this.e, "Removing report " + i + " from PulseCallbackManager");
        g.remove(Integer.valueOf(i));
    }

    public static List<im> c() {
        return new ArrayList(g.values());
    }

    public final synchronized void a(final ij ijVar) {
        kf.a(3, this.e, ijVar.l.m.c + " report sent successfully to " + ijVar.l.k);
        ijVar.f = ik.COMPLETE;
        ijVar.g = "";
        c(ijVar);
        if (kf.c() <= 3 && kf.d()) {
            jr.a().a(new Runnable() { // from class: com.flurry.sdk.il.4
                @Override // java.lang.Runnable
                public final void run() {
                    Toast.makeText(jr.a().a, "PulseCallbackReportInfo HTTP Response Code: " + ijVar.e + " for url: " + ijVar.l.r, 1).show();
                }
            });
        }
    }

    public final synchronized boolean a(ij ijVar, String str) {
        ijVar.h++;
        ijVar.i = System.currentTimeMillis();
        if (!(ijVar.h > ijVar.l.d) && !TextUtils.isEmpty(str)) {
            kf.a(3, this.e, "Report to " + ijVar.l.k + " redirecting to url: " + str);
            ijVar.l.r = str;
            d();
            return true;
        }
        kf.a(3, this.e, "Maximum number of redirects attempted. Aborting: " + ijVar.l.m.c + " report to " + ijVar.l.k);
        ijVar.f = ik.INVALID_RESPONSE;
        ijVar.g = "";
        c(ijVar);
        return false;
    }

    public final synchronized void b(ij ijVar) {
        kf.a(3, this.e, "Maximum number of attempts reached. Aborting: " + ijVar.l.m.c);
        ijVar.f = ik.TIMEOUT;
        ijVar.i = System.currentTimeMillis();
        ijVar.g = "";
        c(ijVar);
    }

    public final synchronized boolean b(ij ijVar, String str) {
        boolean z;
        ijVar.f = ik.INVALID_RESPONSE;
        ijVar.i = System.currentTimeMillis();
        if (str == null) {
            str = "";
        }
        ijVar.g = str;
        ii iiVar = ijVar.l;
        z = false;
        if (iiVar.p >= iiVar.c) {
            kf.a(3, this.e, "Maximum number of attempts reached. Aborting: " + ijVar.l.m.c + " report to " + ijVar.l.k);
            c(ijVar);
        } else if (!ly.h(ijVar.l.r)) {
            kf.a(3, this.e, "Url: " + ijVar.l.r + " is invalid.");
            c(ijVar);
        } else {
            kf.a(3, this.e, "Retrying callback to " + ijVar.l.m.c + " in: " + (ijVar.l.g / 1000) + " seconds.");
            ijVar.a();
            c.incrementAndGet();
            d();
            g();
            z = true;
        }
        return z;
    }

    public final void d() {
        jr.a().b(new Runnable() { // from class: com.flurry.sdk.il.5
            @Override // java.lang.Runnable
            public final void run() throws Throwable {
                il.a();
                List<im> listC = il.c();
                if (il.d == null) {
                    il.m();
                }
                il.d.a(listC);
            }
        });
    }

    public static List<im> e() {
        if (d == null) {
            m();
        }
        return d.a();
    }

    private void c(ij ijVar) {
        ijVar.d = true;
        ijVar.a();
        c.incrementAndGet();
        ijVar.l.c();
        kf.a(3, this.e, ijVar.l.m.c + " report to " + ijVar.l.k + " finalized.");
        d();
        g();
    }

    private void g() {
        if (h() || i()) {
            kf.a(3, this.e, "Threshold reached. Sending callback logging reports");
            j();
        }
    }

    private static boolean h() {
        return c.intValue() >= a;
    }

    private boolean i() {
        return System.currentTimeMillis() > this.i;
    }

    private void j() {
        Iterator<im> it = c().iterator();
        while (true) {
            boolean z = false;
            if (!it.hasNext()) {
                break;
            }
            im next = it.next();
            Iterator<ii> it2 = next.a().iterator();
            while (it2.hasNext()) {
                Iterator<ij> it3 = it2.next().a.iterator();
                while (it3.hasNext()) {
                    ij next2 = it3.next();
                    if (next2.j) {
                        it3.remove();
                    } else if (!next2.f.equals(ik.PENDING_COMPLETION)) {
                        next2.j = true;
                        z = true;
                    }
                }
            }
            if (z) {
                in.a().a(next);
            }
        }
        in.a().b();
        this.i = System.currentTimeMillis() + ((long) b);
        k();
        List<im> listC = c();
        for (int i = 0; i < listC.size(); i++) {
            im imVar = listC.get(i);
            if (imVar.b()) {
                c(imVar.b);
            } else {
                List<ii> listA = imVar.a();
                for (int i2 = 0; i2 < listA.size(); i2++) {
                    ii iiVar = listA.get(i2);
                    if (iiVar.l) {
                        imVar.d.remove(Long.valueOf(iiVar.b));
                    } else {
                        Iterator<ij> it4 = iiVar.a.iterator();
                        while (it4.hasNext()) {
                            if (it4.next().j) {
                                it4.remove();
                            }
                        }
                    }
                }
            }
        }
        c = new AtomicInteger(0);
        d();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void k() {
        SharedPreferences.Editor editorEdit = jr.a().a.getSharedPreferences("FLURRY_SHARED_PREFERENCES", 0).edit();
        editorEdit.putLong("timeToSendNextPulseReport", this.i);
        editorEdit.commit();
    }

    private synchronized int l() {
        return this.h.incrementAndGet();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void m() {
        d = new jy<>(jr.a().a.getFileStreamPath(".yflurryanongoingpulsecallbackreporter"), ".yflurryanongoingpulsecallbackreporter", 2, new lc<List<im>>() { // from class: com.flurry.sdk.il.6
            @Override // com.flurry.sdk.lc
            public final kz<List<im>> a(int i) {
                return new ky(new im.a());
            }
        });
    }
}
