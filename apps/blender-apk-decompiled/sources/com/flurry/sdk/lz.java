package com.flurry.sdk;

import android.content.Context;
import com.flurry.sdk.lj;
import java.lang.Thread;

/* JADX INFO: loaded from: classes.dex */
public class lz implements ki, lj.a, Thread.UncaughtExceptionHandler {
    private static final String a = "lz";
    private boolean b;

    @Override // com.flurry.sdk.ki
    public final void a(Context context) {
        li liVarA = li.a();
        this.b = ((Boolean) liVarA.a("CaptureUncaughtExceptions")).booleanValue();
        liVarA.a("CaptureUncaughtExceptions", (lj.a) this);
        kf.a(4, a, "initSettings, CrashReportingEnabled = " + this.b);
        ma maVarA = ma.a();
        synchronized (maVarA.b) {
            maVarA.b.put(this, null);
        }
    }

    @Override // com.flurry.sdk.ki
    public final void b() {
        ma.b();
        li.a().b("CaptureUncaughtExceptions", this);
    }

    @Override // com.flurry.sdk.lj.a
    public final void a(String str, Object obj) {
        if (str.equals("CaptureUncaughtExceptions")) {
            this.b = ((Boolean) obj).booleanValue();
            kf.a(4, a, "onSettingUpdate, CrashReportingEnabled = " + this.b);
            return;
        }
        kf.a(6, a, "onSettingUpdate internal error!");
    }

    @Override // java.lang.Thread.UncaughtExceptionHandler
    public void uncaughtException(Thread thread, Throwable th) {
        th.printStackTrace();
        if (this.b) {
            String message = "";
            StackTraceElement[] stackTrace = th.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                StringBuilder sb = new StringBuilder();
                if (th.getMessage() != null) {
                    sb.append(" (" + th.getMessage() + ")\n");
                }
                message = sb.toString();
            } else if (th.getMessage() != null) {
                message = th.getMessage();
            }
            hk.a();
            hk.a("uncaught", message, th);
        }
        lf.a().e();
        ji.a().d();
    }
}
