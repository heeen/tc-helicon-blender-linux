package com.flurry.sdk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.os.EnvironmentCompat;
import android.text.TextUtils;
import com.flurry.sdk.lj;

/* JADX INFO: loaded from: classes.dex */
public class jn implements lj.a {
    private static jn a = null;
    private static final String b = "jn";
    private String c;
    private String d;

    public static String h() {
        return "";
    }

    public static synchronized jn a() {
        if (a == null) {
            a = new jn();
        }
        return a;
    }

    public static void b() {
        if (a != null) {
            li.a().b("VersionName", a);
        }
        a = null;
    }

    private jn() {
        li liVarA = li.a();
        this.c = (String) liVarA.a("VersionName");
        liVarA.a("VersionName", (lj.a) this);
        kf.a(4, b, "initSettings, VersionName = " + this.c);
    }

    public static String c() {
        return Build.VERSION.RELEASE;
    }

    public static String d() {
        return Build.DEVICE;
    }

    public static String e() {
        return Build.ID;
    }

    public static String f() {
        return Build.MANUFACTURER;
    }

    public static String g() {
        return Build.MODEL;
    }

    public static String a(Context context) {
        PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            return null;
        }
        try {
            return packageManager.getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException unused) {
            return EnvironmentCompat.MEDIA_UNKNOWN;
        }
    }

    public final synchronized String i() {
        if (!TextUtils.isEmpty(this.c)) {
            return this.c;
        }
        if (!TextUtils.isEmpty(this.d)) {
            return this.d;
        }
        this.d = j();
        return this.d;
    }

    private static String j() {
        try {
            Context context = jr.a().a;
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (packageInfo.versionName != null) {
                return packageInfo.versionName;
            }
            return packageInfo.versionCode != 0 ? Integer.toString(packageInfo.versionCode) : "Unknown";
        } catch (Throwable th) {
            kf.a(6, b, "", th);
            return "Unknown";
        }
    }

    @Override // com.flurry.sdk.lj.a
    public final void a(String str, Object obj) {
        if (str.equals("VersionName")) {
            this.c = (String) obj;
            kf.a(4, b, "onSettingUpdate, VersionName = " + this.c);
            return;
        }
        kf.a(6, b, "onSettingUpdate internal error!");
    }
}
