package com.flurry.sdk;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class kn extends lx {
    static final String e = "kn";
    private int a;
    private int b;
    private HttpURLConnection d;
    public String f;
    public a g;
    public c k;
    public boolean l;
    public Exception o;
    public boolean r;
    public boolean t;
    private boolean x;
    private boolean y;
    public int h = 10000;
    public int i = 15000;
    public boolean j = true;
    private final jw<String, String> c = new jw<>();
    long m = -1;
    public long n = -1;
    public int p = -1;
    public final jw<String, String> q = new jw<>();
    private final Object z = new Object();
    public int s = 25000;
    private km A = new km(this);

    public static class b implements c {
        @Override // com.flurry.sdk.kn.c
        public void a(kn knVar) {
        }

        @Override // com.flurry.sdk.kn.c
        public void a(kn knVar, InputStream inputStream) throws Exception {
        }

        @Override // com.flurry.sdk.kn.c
        public final void a(OutputStream outputStream) throws Exception {
        }
    }

    public interface c {
        void a(kn knVar);

        void a(kn knVar, InputStream inputStream) throws Exception;

        void a(OutputStream outputStream) throws Exception;
    }

    public enum a {
        kUnknown,
        kGet,
        kPost,
        kPut,
        kDelete,
        kHead;

        @Override // java.lang.Enum
        public final String toString() {
            switch (this) {
                case kPost:
                    return "POST";
                case kPut:
                    return "PUT";
                case kDelete:
                    return "DELETE";
                case kHead:
                    return "HEAD";
                case kGet:
                    return "GET";
                default:
                    return null;
            }
        }
    }

    public final void a(String str, String str2) {
        this.c.a(str, str2);
    }

    public final boolean b() {
        boolean z;
        synchronized (this.z) {
            z = this.y;
        }
        return z;
    }

    public final boolean c() {
        return !e() && d();
    }

    public final boolean d() {
        return this.p >= 200 && this.p < 400 && !this.t;
    }

    public final boolean e() {
        return this.o != null;
    }

    public final List<String> a(String str) {
        return this.q.a(str);
    }

    public final void f() {
        kf.a(3, e, "Cancelling http request: " + this.f);
        synchronized (this.z) {
            this.y = true;
        }
        if (this.x) {
            return;
        }
        this.x = true;
        if (this.d != null) {
            new Thread() { // from class: com.flurry.sdk.kn.1
                @Override // java.lang.Thread, java.lang.Runnable
                public final void run() {
                    try {
                        if (kn.this.d != null) {
                            kn.this.d.disconnect();
                        }
                    } catch (Throwable unused) {
                    }
                }
            }.start();
        }
    }

    @Override // com.flurry.sdk.lw
    public void a() {
        try {
            try {
            } catch (Exception e2) {
                kf.a(4, e, "HTTP status: " + this.p + " for url: " + this.f);
                String str = e;
                StringBuilder sb = new StringBuilder("Exception during http request: ");
                sb.append(this.f);
                kf.a(3, str, sb.toString(), e2);
                this.b = this.d.getReadTimeout();
                this.a = this.d.getConnectTimeout();
                this.o = e2;
            }
            if (this.f == null) {
                return;
            }
            if (!jk.a().b) {
                kf.a(3, e, "Network not available, aborting http request: " + this.f);
                return;
            }
            if (this.g == null || a.kUnknown.equals(this.g)) {
                this.g = a.kGet;
            }
            i();
            kf.a(4, e, "HTTP status: " + this.p + " for url: " + this.f);
        } finally {
            this.A.a();
            h();
        }
    }

    @Override // com.flurry.sdk.lx
    public final void g() {
        f();
    }

    private void i() throws Exception {
        BufferedOutputStream bufferedOutputStream;
        Throwable th;
        OutputStream outputStream;
        BufferedInputStream bufferedInputStream;
        Throwable th2;
        InputStream inputStream;
        if (this.y) {
            return;
        }
        this.f = lr.a(this.f);
        try {
            this.d = (HttpURLConnection) new URL(this.f).openConnection();
            this.d.setConnectTimeout(this.h);
            this.d.setReadTimeout(this.i);
            this.d.setRequestMethod(this.g.toString());
            this.d.setInstanceFollowRedirects(this.j);
            this.d.setDoOutput(a.kPost.equals(this.g));
            this.d.setDoInput(true);
            for (Map.Entry<String, String> entry : this.c.b()) {
                this.d.addRequestProperty(entry.getKey(), entry.getValue());
            }
            if (!a.kGet.equals(this.g) && !a.kPost.equals(this.g)) {
                this.d.setRequestProperty("Accept-Encoding", "");
            }
            if (this.y) {
                return;
            }
            if (a.kPost.equals(this.g)) {
                try {
                    outputStream = this.d.getOutputStream();
                    try {
                        bufferedOutputStream = new BufferedOutputStream(outputStream);
                        try {
                            if (this.k != null && !b()) {
                                this.k.a(bufferedOutputStream);
                            }
                            lr.a(bufferedOutputStream);
                            lr.a(outputStream);
                        } catch (Throwable th3) {
                            th = th3;
                            lr.a(bufferedOutputStream);
                            lr.a(outputStream);
                            throw th;
                        }
                    } catch (Throwable th4) {
                        bufferedOutputStream = null;
                        th = th4;
                    }
                } catch (Throwable th5) {
                    bufferedOutputStream = null;
                    th = th5;
                    outputStream = null;
                }
            }
            if (this.l) {
                this.m = System.currentTimeMillis();
            }
            if (this.r) {
                this.A.a(this.s);
            }
            this.p = this.d.getResponseCode();
            if (this.l && this.m != -1) {
                this.n = System.currentTimeMillis() - this.m;
            }
            this.A.a();
            for (Map.Entry<String, List<String>> entry2 : this.d.getHeaderFields().entrySet()) {
                Iterator<String> it = entry2.getValue().iterator();
                while (it.hasNext()) {
                    this.q.a(entry2.getKey(), it.next());
                }
            }
            if (!a.kGet.equals(this.g) && !a.kPost.equals(this.g)) {
                return;
            }
            if (this.y) {
                return;
            }
            try {
                inputStream = this.d.getInputStream();
                try {
                    bufferedInputStream = new BufferedInputStream(inputStream);
                    try {
                        if (this.k != null && !b()) {
                            this.k.a(this, bufferedInputStream);
                        }
                        lr.a((Closeable) bufferedInputStream);
                        lr.a((Closeable) inputStream);
                    } catch (Throwable th6) {
                        th2 = th6;
                        lr.a((Closeable) bufferedInputStream);
                        lr.a((Closeable) inputStream);
                        throw th2;
                    }
                } catch (Throwable th7) {
                    bufferedInputStream = null;
                    th2 = th7;
                }
            } catch (Throwable th8) {
                bufferedInputStream = null;
                th2 = th8;
                inputStream = null;
            }
        } finally {
            j();
        }
    }

    final void h() {
        if (this.k == null || b()) {
            return;
        }
        this.k.a(this);
    }

    private void j() {
        if (this.x) {
            return;
        }
        this.x = true;
        if (this.d != null) {
            this.d.disconnect();
        }
    }
}
