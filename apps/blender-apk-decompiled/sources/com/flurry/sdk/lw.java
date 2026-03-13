package com.flurry.sdk;

import java.io.PrintStream;
import java.io.PrintWriter;

/* JADX INFO: loaded from: classes.dex */
public abstract class lw implements Runnable {
    private static final String a = "lw";
    PrintStream u;
    PrintWriter v;

    public abstract void a();

    @Override // java.lang.Runnable
    public final void run() {
        try {
            a();
        } catch (Throwable th) {
            if (this.u != null) {
                th.printStackTrace(this.u);
            } else if (this.v != null) {
                th.printStackTrace(this.v);
            } else {
                th.printStackTrace();
            }
            kf.a(6, a, "", th);
        }
    }
}
