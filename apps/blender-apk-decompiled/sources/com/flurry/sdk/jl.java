package com.flurry.sdk;

import android.telephony.TelephonyManager;

/* JADX INFO: loaded from: classes.dex */
public class jl {
    private static jl a = null;
    private static final String b = "jl";

    public static synchronized jl a() {
        if (a == null) {
            a = new jl();
        }
        return a;
    }

    public static void b() {
        a = null;
    }

    private jl() {
    }

    public static String c() {
        TelephonyManager telephonyManager = (TelephonyManager) jr.a().a.getSystemService("phone");
        if (telephonyManager == null) {
            return null;
        }
        return telephonyManager.getNetworkOperatorName();
    }

    public static String d() {
        TelephonyManager telephonyManager = (TelephonyManager) jr.a().a.getSystemService("phone");
        if (telephonyManager == null) {
            return null;
        }
        return telephonyManager.getNetworkOperator();
    }
}
