package com.flurry.sdk;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class kb {
    private static kb a;
    private final jw<String, ko<ka<?>>> b = new jw<>();
    private final jw<ko<ka<?>>, String> c = new jw<>();

    public static synchronized kb a() {
        if (a == null) {
            a = new kb();
        }
        return a;
    }

    public static synchronized void b() {
        if (a != null) {
            a.c();
            a = null;
        }
    }

    private kb() {
    }

    public final synchronized void a(String str, ka<?> kaVar) {
        if (!TextUtils.isEmpty(str) && kaVar != null) {
            ko<ka<?>> koVar = new ko<>(kaVar);
            List<ko<ka<?>>> listA = this.b.a(str, false);
            if (listA != null ? listA.contains(koVar) : false) {
                return;
            }
            this.b.a(str, koVar);
            this.c.a(koVar, str);
        }
    }

    public final synchronized void b(String str, ka<?> kaVar) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        ko<ka<?>> koVar = new ko<>(kaVar);
        this.b.b(str, koVar);
        this.c.b(koVar, str);
    }

    public final synchronized void a(String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        Iterator<ko<ka<?>>> it = this.b.a(str).iterator();
        while (it.hasNext()) {
            this.c.b(it.next(), str);
        }
        this.b.b(str);
    }

    public final synchronized void a(ka<?> kaVar) {
        if (kaVar == null) {
            return;
        }
        ko<ka<?>> koVar = new ko<>(kaVar);
        Iterator<String> it = this.c.a(koVar).iterator();
        while (it.hasNext()) {
            this.b.b(it.next(), koVar);
        }
        this.c.b(koVar);
    }

    private synchronized void c() {
        this.b.a();
        this.c.a();
    }

    public final synchronized int b(String str) {
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        return this.b.a(str).size();
    }

    private synchronized List<ka<?>> c(String str) {
        if (TextUtils.isEmpty(str)) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        Iterator<ko<ka<?>>> it = this.b.a(str).iterator();
        while (it.hasNext()) {
            ka kaVar = (ka) it.next().get();
            if (kaVar == null) {
                it.remove();
            } else {
                arrayList.add(kaVar);
            }
        }
        return arrayList;
    }

    public final void a(final jz jzVar) {
        if (jzVar == null) {
            return;
        }
        for (final ka<?> kaVar : c(jzVar.a())) {
            jr.a().b(new lw() { // from class: com.flurry.sdk.kb.1
                @Override // com.flurry.sdk.lw
                public final void a() {
                    kaVar.a(jzVar);
                }
            });
        }
    }
}
