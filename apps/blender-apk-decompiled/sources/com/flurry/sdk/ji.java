package com.flurry.sdk;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import com.flurry.sdk.lj;

/* JADX INFO: loaded from: classes.dex */
public class ji implements lj.a {
    private static ji a = null;
    private static final String b = "ji";
    private boolean g;
    private Location h;
    private Location l;
    private final int c = 3;
    private final long d = 10000;
    private final long e = 90000;
    private final long f = 0;
    private long i = 0;
    private boolean m = false;
    private int n = 0;
    private ka<ll> o = new ka<ll>() { // from class: com.flurry.sdk.ji.1
        @Override // com.flurry.sdk.ka
        public final /* synthetic */ void a(jz jzVar) {
            if (ji.this.i <= 0 || ji.this.i >= System.currentTimeMillis()) {
                return;
            }
            kf.a(4, ji.b, "No location received in 90 seconds , stopping LocationManager");
            ji.this.g();
        }
    };
    private LocationManager j = (LocationManager) jr.a().a.getSystemService("location");
    private a k = new a();

    static /* synthetic */ int c(ji jiVar) {
        int i = jiVar.n + 1;
        jiVar.n = i;
        return i;
    }

    public static synchronized ji a() {
        if (a == null) {
            a = new ji();
        }
        return a;
    }

    public static void b() {
        if (a != null) {
            li.a().b("ReportLocation", a);
            li.a().b("ExplicitLocation", a);
        }
        a = null;
    }

    private ji() {
        li liVarA = li.a();
        this.g = ((Boolean) liVarA.a("ReportLocation")).booleanValue();
        liVarA.a("ReportLocation", (lj.a) this);
        kf.a(4, b, "initSettings, ReportLocation = " + this.g);
        this.h = (Location) liVarA.a("ExplicitLocation");
        liVarA.a("ExplicitLocation", (lj.a) this);
        kf.a(4, b, "initSettings, ExplicitLocation = " + this.h);
    }

    public final synchronized void c() {
        kf.a(4, b, "Location update requested");
        if (this.n < 3 && !this.m && this.g && this.h == null) {
            Context context = jr.a().a;
            if (context.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0 || context.checkCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == 0) {
                this.n = 0;
                String str = null;
                if (a(context)) {
                    str = "passive";
                } else if (b(context)) {
                    str = "network";
                }
                if (!TextUtils.isEmpty(str)) {
                    this.j.requestLocationUpdates(str, 10000L, 0.0f, this.k, Looper.getMainLooper());
                }
                this.l = a(str);
                this.i = System.currentTimeMillis() + 90000;
                kf.a(4, b, "Register location timer");
                lm.a().a(this.o);
                this.m = true;
                kf.a(4, b, "LocationProvider started");
            }
        }
    }

    public final synchronized void d() {
        kf.a(4, b, "Stop update location requested");
        g();
    }

    public final Location e() {
        String str;
        if (this.h != null) {
            return this.h;
        }
        Location location = null;
        if (this.g) {
            Context context = jr.a().a;
            if (!a(context) && !b(context)) {
                return null;
            }
            if (a(context)) {
                str = "passive";
            } else {
                str = b(context) ? "network" : null;
            }
            if (str != null) {
                Location locationA = a(str);
                if (locationA != null) {
                    this.l = locationA;
                }
                location = this.l;
            }
        }
        kf.a(4, b, "getLocation() = " + location);
        return location;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void g() {
        if (this.m) {
            this.j.removeUpdates(this.k);
            this.n = 0;
            this.i = 0L;
            kf.a(4, b, "Unregister location timer");
            lm.a().b(this.o);
            this.m = false;
            kf.a(4, b, "LocationProvider stopped");
        }
    }

    private static boolean a(Context context) {
        return context.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0;
    }

    private static boolean b(Context context) {
        return context.checkCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == 0;
    }

    private Location a(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return this.j.getLastKnownLocation(str);
    }

    class a implements LocationListener {
        @Override // android.location.LocationListener
        public final void onProviderDisabled(String str) {
        }

        @Override // android.location.LocationListener
        public final void onProviderEnabled(String str) {
        }

        @Override // android.location.LocationListener
        public final void onStatusChanged(String str, int i, Bundle bundle) {
        }

        public a() {
        }

        @Override // android.location.LocationListener
        public final void onLocationChanged(Location location) {
            if (location != null) {
                ji.this.l = location;
            }
            if (ji.c(ji.this) >= 3) {
                kf.a(4, ji.b, "Max location reports reached, stopping");
                ji.this.g();
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x0023  */
    @Override // com.flurry.sdk.lj.a
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final void a(java.lang.String r3, java.lang.Object r4) {
        /*
            r2 = this;
            int r0 = r3.hashCode()
            r1 = -864112343(0xffffffffcc7eb129, float:-6.6765988E7)
            if (r0 == r1) goto L19
            r1 = -300729815(0xffffffffee133a29, float:-1.1391152E28)
            if (r0 == r1) goto Lf
            goto L23
        Lf:
            java.lang.String r0 = "ExplicitLocation"
            boolean r3 = r3.equals(r0)
            if (r3 == 0) goto L23
            r3 = 1
            goto L24
        L19:
            java.lang.String r0 = "ReportLocation"
            boolean r3 = r3.equals(r0)
            if (r3 == 0) goto L23
            r3 = 0
            goto L24
        L23:
            r3 = -1
        L24:
            r0 = 4
            switch(r3) {
                case 0: goto L4b;
                case 1: goto L31;
                default: goto L28;
            }
        L28:
            r2 = 6
            java.lang.String r3 = com.flurry.sdk.ji.b
            java.lang.String r4 = "LocationProvider internal error! Had to be LocationCriteria, ReportLocation or ExplicitLocation key."
            com.flurry.sdk.kf.a(r2, r3, r4)
            return
        L31:
            android.location.Location r4 = (android.location.Location) r4
            r2.h = r4
            java.lang.String r3 = com.flurry.sdk.ji.b
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            java.lang.String r1 = "onSettingUpdate, ExplicitLocation = "
            r4.<init>(r1)
            android.location.Location r2 = r2.h
            r4.append(r2)
            java.lang.String r2 = r4.toString()
            com.flurry.sdk.kf.a(r0, r3, r2)
            return
        L4b:
            java.lang.Boolean r4 = (java.lang.Boolean) r4
            boolean r3 = r4.booleanValue()
            r2.g = r3
            java.lang.String r3 = com.flurry.sdk.ji.b
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            java.lang.String r1 = "onSettingUpdate, ReportLocation = "
            r4.<init>(r1)
            boolean r2 = r2.g
            r4.append(r2)
            java.lang.String r2 = r4.toString()
            com.flurry.sdk.kf.a(r0, r3, r2)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.flurry.sdk.ji.a(java.lang.String, java.lang.Object):void");
    }
}
