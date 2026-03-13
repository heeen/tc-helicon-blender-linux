package com.flurry.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/* JADX INFO: loaded from: classes.dex */
public final class jk extends BroadcastReceiver {
    private static jk c;
    boolean a;
    public boolean b;
    private boolean d;

    public static synchronized jk a() {
        if (c == null) {
            c = new jk();
        }
        return c;
    }

    public static synchronized void b() {
        if (c != null) {
            c.e();
        }
        c = null;
    }

    public enum a {
        NONE_OR_UNKNOWN(0),
        NETWORK_AVAILABLE(1),
        WIFI(2),
        CELL(3);

        public int e;

        a(int i) {
            this.e = i;
        }
    }

    private jk() {
        this.d = false;
        Context context = jr.a().a;
        this.d = context.checkCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE") == 0;
        this.b = a(context);
        if (this.d) {
            d();
        }
    }

    private synchronized void d() {
        if (this.a) {
            return;
        }
        Context context = jr.a().a;
        this.b = a(context);
        context.registerReceiver(this, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        this.a = true;
    }

    private synchronized void e() {
        if (this.a) {
            jr.a().a.unregisterReceiver(this);
            this.a = false;
        }
    }

    @Override // android.content.BroadcastReceiver
    public final void onReceive(Context context, Intent intent) {
        boolean zA = a(context);
        if (this.b != zA) {
            this.b = zA;
            jj jjVar = new jj();
            jjVar.a = zA;
            jjVar.b = c();
            kb.a().a(jjVar);
        }
    }

    private boolean a(Context context) {
        if (!this.d || context == null) {
            return true;
        }
        NetworkInfo activeNetworkInfo = f().getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public final a c() {
        if (!this.d) {
            return a.NONE_OR_UNKNOWN;
        }
        NetworkInfo activeNetworkInfo = f().getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            return a.NONE_OR_UNKNOWN;
        }
        int type = activeNetworkInfo.getType();
        if (type != 8) {
            switch (type) {
                case 0:
                case 2:
                case 3:
                case 4:
                case 5:
                    return a.CELL;
                case 1:
                    return a.WIFI;
                default:
                    if (activeNetworkInfo.isConnected()) {
                        return a.NETWORK_AVAILABLE;
                    }
                    return a.NONE_OR_UNKNOWN;
            }
        }
        return a.NONE_OR_UNKNOWN;
    }

    private static ConnectivityManager f() {
        return (ConnectivityManager) jr.a().a.getSystemService("connectivity");
    }
}
