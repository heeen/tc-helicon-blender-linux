package com.flurry.sdk;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public final class jp extends ke<kn> {
    private static jp a;

    public static synchronized jp a() {
        if (a == null) {
            a = new jp();
        }
        return a;
    }

    public static synchronized void b() {
        if (a != null) {
            a.c();
        }
        a = null;
    }

    protected jp() {
        super(jp.class.getName(), TimeUnit.MILLISECONDS, new PriorityBlockingQueue(11, new kc()));
    }
}
