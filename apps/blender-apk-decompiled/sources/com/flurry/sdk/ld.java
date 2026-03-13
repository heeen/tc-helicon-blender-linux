package com.flurry.sdk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class ld {
    private static final List<Class<?>> b = new ArrayList();
    private final String a = ld.class.getSimpleName();
    private final Map<Class<?>, Object> c = new LinkedHashMap();

    public static void a(Class<?> cls) {
        synchronized (b) {
            b.add(cls);
        }
    }

    public static void b(Class<?> cls) {
        synchronized (b) {
            b.remove(cls);
        }
    }

    public ld() {
        ArrayList<Class<?>> arrayList;
        synchronized (b) {
            arrayList = new ArrayList(b);
        }
        for (Class<?> cls : arrayList) {
            try {
                Object objNewInstance = cls.newInstance();
                synchronized (this.c) {
                    this.c.put(cls, objNewInstance);
                }
            } catch (Exception e) {
                kf.a(5, this.a, "Module data " + cls + " is not available:", e);
            }
        }
    }

    public final Object c(Class<?> cls) {
        Object obj;
        synchronized (this.c) {
            obj = this.c.get(cls);
        }
        return obj;
    }
}
