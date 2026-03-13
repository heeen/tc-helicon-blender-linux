package com.flurry.sdk;

import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class jt {
    private static jt a = null;
    private static final String b = "jt";
    private static final HashMap<String, Map<String, String>> c = new HashMap<>();

    public static synchronized jt a() {
        if (a == null) {
            a = new jt();
        }
        return a;
    }

    public static synchronized void b() {
        a = null;
    }

    public final synchronized void a(String str, String str2, Map<String, String> map) {
        if (map == null) {
            try {
                map = new HashMap<>();
            } catch (Throwable th) {
                throw th;
            }
        }
        if (map.size() >= 10) {
            kf.e(b, "MaxOriginParams exceeded: " + map.size());
            return;
        }
        map.put("flurryOriginVersion", str2);
        synchronized (c) {
            if (c.size() >= 10 && !c.containsKey(str)) {
                kf.e(b, "MaxOrigins exceeded: " + c.size());
                return;
            }
            c.put(str, map);
        }
    }

    public final synchronized HashMap<String, Map<String, String>> c() {
        HashMap<String, Map<String, String>> map;
        synchronized (c) {
            map = new HashMap<>(c);
        }
        return map;
    }
}
