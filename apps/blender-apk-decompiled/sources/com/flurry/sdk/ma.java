package com.flurry.sdk;

import java.lang.Thread;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/* JADX INFO: loaded from: classes.dex */
public final class ma {
    private static ma c;
    final Map<Thread.UncaughtExceptionHandler, Void> b = new WeakHashMap();
    final Thread.UncaughtExceptionHandler a = Thread.getDefaultUncaughtExceptionHandler();

    public static synchronized ma a() {
        if (c == null) {
            c = new ma();
        }
        return c;
    }

    public static synchronized void b() {
        if (c != null) {
            Thread.setDefaultUncaughtExceptionHandler(c.a);
        }
        c = null;
    }

    final Set<Thread.UncaughtExceptionHandler> c() {
        Set<Thread.UncaughtExceptionHandler> setKeySet;
        synchronized (this.b) {
            setKeySet = this.b.keySet();
        }
        return setKeySet;
    }

    private ma() {
        Thread.setDefaultUncaughtExceptionHandler(new a(this, (byte) 0));
    }

    final class a implements Thread.UncaughtExceptionHandler {
        private a() {
        }

        /* synthetic */ a(ma maVar, byte b) {
            this();
        }

        @Override // java.lang.Thread.UncaughtExceptionHandler
        public final void uncaughtException(Thread thread, Throwable th) {
            Iterator<Thread.UncaughtExceptionHandler> it = ma.this.c().iterator();
            while (it.hasNext()) {
                try {
                    it.next().uncaughtException(thread, th);
                } catch (Throwable unused) {
                }
            }
            ma maVar = ma.this;
            if (maVar.a != null) {
                try {
                    maVar.a.uncaughtException(thread, th);
                } catch (Throwable unused2) {
                }
            }
        }
    }
}
