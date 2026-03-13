package com.flurry.sdk;

import android.content.Context;
import com.flurry.android.FlurryEventRecordStatus;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class hk implements ki {
    private static final String d = "hk";
    public Cif a;
    public ix b;
    public ih c;

    public static synchronized hk a() {
        return (hk) jr.a().a(hk.class);
    }

    @Override // com.flurry.sdk.ki
    public final void a(Context context) {
        ld.a(ja.class);
        this.b = new ix();
        this.a = new Cif();
        this.c = new ih();
        if (!lr.a(context, "android.permission.INTERNET")) {
            kf.b(d, "Application must declare permission: android.permission.INTERNET");
        }
        if (lr.a(context, "android.permission.ACCESS_NETWORK_STATE")) {
            return;
        }
        kf.e(d, "It is highly recommended that the application declare permission: android.permission.ACCESS_NETWORK_STATE");
    }

    @Override // com.flurry.sdk.ki
    public final void b() {
        if (this.c != null) {
            this.c.c();
            this.c = null;
        }
        if (this.b != null) {
            ix ixVar = this.b;
            li.a().b("UseHttps", ixVar);
            li.a().b("ReportUrl", ixVar);
            this.b = null;
        }
        if (this.a != null) {
            Cif cif = this.a;
            jr.a().c(cif.a);
            kb.a().b("com.flurry.android.sdk.NetworkStateEvent", cif.d);
            kb.a().b("com.flurry.android.sdk.IdProviderUpdatedAdvertisingId", cif.c);
            kb.a().b("com.flurry.android.sdk.IdProviderFinishedEvent", cif.b);
            il.b();
            li.a().b("ProtonEnabled", cif);
            this.a = null;
        }
        ld.b(ja.class);
    }

    public static FlurryEventRecordStatus a(String str, String str2, Map<String, String> map) {
        ja jaVarC = c();
        return jaVarC != null ? jaVarC.a(str, jc.a(str2), map) : FlurryEventRecordStatus.kFlurryEventFailed;
    }

    public static void a(String str, String str2, Throwable th) {
        ja jaVarC = c();
        if (jaVarC != null) {
            jaVarC.a(str, str2, th.getClass().getName(), th);
        }
    }

    public static ja c() {
        ld ldVarC = lf.a().c();
        if (ldVarC == null) {
            return null;
        }
        return (ja) ldVarC.c(ja.class);
    }
}
