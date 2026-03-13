package com.flurry.sdk;

import com.flurry.sdk.ii;
import com.flurry.sdk.kl;
import com.flurry.sdk.kn;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class ih extends kq<ii> {
    public static long a = 0;
    private static final String e = "ih";

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Type inference failed for: r1v23, types: [RequestObjectType, byte[]] */
    @Override // com.flurry.sdk.kq
    public final /* synthetic */ void a(kp kpVar) {
        final ii iiVar = (ii) kpVar;
        kf.a(3, e, "Sending next pulse report to " + iiVar.k + " at: " + iiVar.r);
        jd.a();
        long jD = jd.d();
        if (jD == 0) {
            jD = a;
        }
        long j = jD;
        jd.a();
        long jG = jd.g();
        if (jG == 0) {
            jG = System.currentTimeMillis() - j;
        }
        final ij ijVar = new ij(iiVar, j, jG, iiVar.p);
        kl klVar = new kl();
        klVar.f = iiVar.r;
        klVar.w = 100000;
        if (iiVar.e.equals(ip.POST)) {
            klVar.c = new kv();
            if (iiVar.j != null) {
                klVar.b = iiVar.j.getBytes();
            }
            klVar.g = kn.a.kPost;
        } else {
            klVar.g = kn.a.kGet;
        }
        klVar.h = iiVar.h * 1000;
        klVar.i = iiVar.i * 1000;
        klVar.l = true;
        klVar.r = true;
        klVar.s = (iiVar.h + iiVar.i) * 1000;
        Map<String, String> map = iiVar.f;
        if (map != null) {
            for (String str : iiVar.f.keySet()) {
                klVar.a(str, map.get(str));
            }
        }
        klVar.j = false;
        klVar.a = new kl.a<byte[], String>() { // from class: com.flurry.sdk.ih.2
            @Override // com.flurry.sdk.kl.a
            public final /* synthetic */ void a(kl<byte[], String> klVar2, String str2) {
                String str3 = str2;
                kf.a(3, ih.e, "Pulse report to " + iiVar.k + " for " + iiVar.m.c + ", HTTP status code is: " + klVar2.p);
                int i = klVar2.p;
                ij ijVar2 = ijVar;
                int i2 = (int) klVar2.n;
                if (i2 >= 0) {
                    ijVar2.k += (long) i2;
                } else if (ijVar2.k <= 0) {
                    ijVar2.k = 0L;
                }
                ijVar.e = i;
                if (klVar2.c()) {
                    if (i >= 200 && i < 300) {
                        ih.b(ih.this, ijVar, iiVar);
                        return;
                    }
                    if (i < 300 || i >= 400) {
                        kf.a(3, ih.e, iiVar.m.c + " report failed sending to : " + iiVar.k);
                        ih.a(ih.this, ijVar, iiVar, str3);
                        return;
                    }
                    ih.a(ih.this, ijVar, iiVar, klVar2);
                    return;
                }
                Exception exc = klVar2.o;
                boolean z = true;
                boolean z2 = klVar2.o != null && (klVar2.o instanceof SocketTimeoutException);
                if (!klVar2.t && !z2) {
                    z = false;
                }
                if (!z) {
                    kf.a(3, ih.e, "Error occured when trying to connect to: " + iiVar.k + ". Exception: " + exc.getMessage());
                    ih.a(ih.this, ijVar, iiVar, str3);
                    return;
                }
                if (klVar2.e()) {
                    kf.a(3, ih.e, "Timeout occured when trying to connect to: " + iiVar.k + ". Exception: " + klVar2.o.getMessage());
                } else {
                    kf.a(3, ih.e, "Manually managed http request timeout occured for: " + iiVar.k);
                }
                ih.a(ih.this, ijVar, iiVar);
            }
        };
        jp.a().a(this, klVar);
    }

    public ih() {
        kq.b = 30000L;
        this.d = kq.b;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.flurry.sdk.kq
    public final jy<List<ii>> a() {
        return new jy<>(jr.a().a.getFileStreamPath(".yflurryanpulsecallbackreporter"), ".yflurryanpulsecallbackreporter", 2, new lc<List<ii>>() { // from class: com.flurry.sdk.ih.1
            @Override // com.flurry.sdk.lc
            public final kz<List<ii>> a(int i) {
                return new ky(new ii.a());
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.flurry.sdk.kq
    public final synchronized void a(List<ii> list) {
        il.a().d();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.flurry.sdk.kq
    public final synchronized void b(List<ii> list) {
        il.a();
        List<im> listE = il.e();
        if (listE == null) {
            return;
        }
        if (listE.size() == 0) {
            return;
        }
        kf.a(3, e, "Restoring " + listE.size() + " from report queue.");
        Iterator<im> it = listE.iterator();
        while (it.hasNext()) {
            il.a().b(it.next());
        }
        il.a();
        Iterator<im> it2 = il.c().iterator();
        while (it2.hasNext()) {
            for (ii iiVar : it2.next().a()) {
                if (!iiVar.l) {
                    kf.a(3, e, "Callback for " + iiVar.m.c + " to " + iiVar.k + " not completed.  Adding to reporter queue.");
                    list.add(iiVar);
                }
            }
        }
    }

    static /* synthetic */ void a(ih ihVar, ij ijVar, ii iiVar) {
        il.a().b(ijVar);
        ihVar.c(iiVar);
    }

    static /* synthetic */ void a(ih ihVar, ij ijVar, ii iiVar, String str) {
        boolean zB = il.a().b(ijVar, str);
        kf.a(3, e, "Failed report retrying: " + zB);
        if (zB) {
            ihVar.d(iiVar);
        } else {
            ihVar.c(iiVar);
        }
    }

    static /* synthetic */ void b(ih ihVar, ij ijVar, ii iiVar) {
        kf.a(3, e, iiVar.m.c + " report sent successfully to : " + iiVar.k);
        il.a().a(ijVar);
        ihVar.c(iiVar);
    }

    static /* synthetic */ void a(ih ihVar, ij ijVar, ii iiVar, kl klVar) {
        List<String> listA = klVar.a("Location");
        String strB = (listA == null || listA.size() <= 0) ? null : ly.b(listA.get(0), iiVar.q);
        boolean zA = il.a().a(ijVar, strB);
        if (zA) {
            kf.a(3, e, "Received redirect url. Retrying: " + strB);
        } else {
            kf.a(3, e, "Received redirect url. Retrying: false");
        }
        if (!zA) {
            ihVar.c(iiVar);
            return;
        }
        iiVar.r = strB;
        klVar.f = strB;
        if (klVar.q != null && klVar.q.a.containsKey("Location")) {
            klVar.q.b("Location");
        }
        jp.a().a(ihVar, klVar);
    }
}
