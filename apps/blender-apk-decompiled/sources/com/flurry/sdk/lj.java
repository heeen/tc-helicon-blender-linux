package com.flurry.sdk;

import android.text.TextUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class lj {
    private Map<String, Object> a = new HashMap();
    private Map<String, List<a>> b = new HashMap();

    public interface a {
        void a(String str, Object obj);
    }

    public final synchronized void a(String str, Object obj) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        Object obj2 = this.a.get(str);
        if (!(obj == obj2 || (obj != null && obj.equals(obj2)))) {
            if (obj == null) {
                this.a.remove(str);
            } else {
                this.a.put(str, obj);
            }
            if (this.b.get(str) != null) {
                Iterator<a> it = this.b.get(str).iterator();
                while (it.hasNext()) {
                    it.next().a(str, obj);
                }
            }
        }
    }

    public final synchronized Object a(String str) {
        return this.a.get(str);
    }

    public final synchronized void a(String str, a aVar) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        if (aVar == null) {
            return;
        }
        List<a> linkedList = this.b.get(str);
        if (linkedList == null) {
            linkedList = new LinkedList<>();
        }
        linkedList.add(aVar);
        this.b.put(str, linkedList);
    }

    public final synchronized boolean b(String str, a aVar) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        if (aVar == null) {
            return false;
        }
        List<a> list = this.b.get(str);
        if (list == null) {
            return false;
        }
        return list.remove(aVar);
    }

    public final synchronized void c() {
        this.b.clear();
    }
}
