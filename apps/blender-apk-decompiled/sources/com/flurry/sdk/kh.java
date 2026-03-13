package com.flurry.sdk;

import android.content.Context;
import android.os.Build;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public final class kh {
    private static final String a = "kh";
    private static final Map<Class<? extends ki>, kg> b = new LinkedHashMap();
    private final Map<Class<? extends ki>, ki> c = new LinkedHashMap();

    public static void a(Class<? extends ki> cls) {
        if (cls == null) {
            return;
        }
        synchronized (b) {
            b.put(cls, new kg(cls));
        }
    }

    public final synchronized void a(Context context) {
        ArrayList<kg> arrayList;
        if (context == null) {
            kf.a(5, a, "Null context.");
            return;
        }
        synchronized (b) {
            arrayList = new ArrayList(b.values());
        }
        for (kg kgVar : arrayList) {
            try {
                if (kgVar.a != null && Build.VERSION.SDK_INT >= kgVar.b) {
                    ki kiVarNewInstance = kgVar.a.newInstance();
                    kiVarNewInstance.a(context);
                    this.c.put(kgVar.a, kiVarNewInstance);
                }
            } catch (Exception e) {
                kf.a(5, a, "Flurry Module for class " + kgVar.a + " is not available:", e);
            }
        }
        lf.a().a(context);
        jv.a();
    }

    public final synchronized void a() {
        jv.b();
        lf.b();
        List<ki> listB = b();
        for (int size = listB.size() - 1; size >= 0; size--) {
            try {
                this.c.remove(listB.get(size).getClass()).b();
            } catch (Exception e) {
                kf.a(5, a, "Error destroying module:", e);
            }
        }
    }

    public final ki b(Class<? extends ki> cls) {
        ki kiVar;
        if (cls == null) {
            return null;
        }
        synchronized (this.c) {
            kiVar = this.c.get(cls);
        }
        if (kiVar != null) {
            return kiVar;
        }
        throw new IllegalStateException("Module was not registered/initialized. " + cls);
    }

    private List<ki> b() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.c) {
            arrayList.addAll(this.c.values());
        }
        return arrayList;
    }
}
