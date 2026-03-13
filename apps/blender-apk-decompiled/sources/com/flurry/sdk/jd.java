package com.flurry.sdk;

import android.content.Context;
import com.flurry.sdk.jk;

/* JADX INFO: loaded from: classes.dex */
public class jd implements ki {
    private static final String a = "jd";

    public static synchronized jd a() {
        return (jd) jr.a().a(jd.class);
    }

    @Override // com.flurry.sdk.ki
    public final void a(Context context) {
        ld.a(jq.class);
        kb.a();
        lm.a();
        li.a();
        jt.a();
        jk.a();
        je.a();
        jl.a();
        ji.a();
        je.a();
        jn.a();
        jh.a();
        jp.a();
    }

    @Override // com.flurry.sdk.ki
    public final void b() {
        jp.b();
        jh.a = null;
        jn.b();
        je.b();
        ji.b();
        jl.b();
        je.b();
        jk.b();
        jt.b();
        li.b();
        lm.b();
        kb.b();
        ld.b(jq.class);
    }

    public static String c() {
        jq jqVarI = i();
        if (jqVarI != null) {
            return Long.toString(jqVarI.c);
        }
        return null;
    }

    public static long d() {
        jq jqVarI = i();
        if (jqVarI != null) {
            return jqVarI.c;
        }
        return 0L;
    }

    public static long e() {
        jq jqVarI = i();
        if (jqVarI != null) {
            return jqVarI.d;
        }
        return 0L;
    }

    public static long f() {
        jq jqVarI = i();
        if (jqVarI != null) {
            return jqVarI.e;
        }
        return -1L;
    }

    public static long g() {
        jq jqVarI = i();
        if (jqVarI != null) {
            return jqVarI.c();
        }
        return 0L;
    }

    public static jk.a h() {
        return jk.a().c();
    }

    public static jq i() {
        ld ldVarC = lf.a().c();
        if (ldVarC == null) {
            return null;
        }
        return (jq) ldVarC.c(jq.class);
    }
}
