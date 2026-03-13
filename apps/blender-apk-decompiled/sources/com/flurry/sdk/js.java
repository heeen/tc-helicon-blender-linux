package com.flurry.sdk;

import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
public class js {
    private static final String a = "js";

    public static int a() {
        int iIntValue = ((Integer) li.a().a("AgentVersion")).intValue();
        kf.a(4, a, "getAgentVersion() = " + iIntValue);
        return iIntValue;
    }

    private static String c() {
        return (String) li.a().a("ReleaseBetaVersion");
    }

    public static String b() {
        return String.format(Locale.getDefault(), "Flurry_Android_%d_%d.%d.%d%s%s", Integer.valueOf(a()), Integer.valueOf(((Integer) li.a().a("ReleaseMajorVersion")).intValue()), Integer.valueOf(((Integer) li.a().a("ReleaseMinorVersion")).intValue()), Integer.valueOf(((Integer) li.a().a("ReleasePatchVersion")).intValue()), c().length() > 0 ? "." : "", c());
    }
}
