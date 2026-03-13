package com.google.android.gms.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/* JADX INFO: loaded from: classes.dex */
final class zzfjn {
    private static final zzfjn zzprm = new zzfjn();
    private final zzfjw zzprn;
    private final ConcurrentMap<Class<?>, zzfjv<?>> zzpro = new ConcurrentHashMap();

    private zzfjn() {
        String[] strArr = {"com.google.protobuf.AndroidProto3SchemaFactory"};
        zzfjw zzfjwVarZzua = null;
        for (int i = 0; i <= 0; i++) {
            zzfjwVarZzua = zzua(strArr[0]);
            if (zzfjwVarZzua != null) {
                break;
            }
        }
        this.zzprn = zzfjwVarZzua == null ? new zzfiq() : zzfjwVarZzua;
    }

    public static zzfjn zzdbf() {
        return zzprm;
    }

    private static zzfjw zzua(String str) {
        try {
            return (zzfjw) Class.forName(str).getConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (Throwable unused) {
            return null;
        }
    }

    public final <T> zzfjv<T> zzl(Class<T> cls) {
        zzfhz.zzc(cls, "messageType");
        zzfjv<T> zzfjvVar = (zzfjv) this.zzpro.get(cls);
        if (zzfjvVar != null) {
            return zzfjvVar;
        }
        zzfjv<T> zzfjvVarZzk = this.zzprn.zzk(cls);
        zzfhz.zzc(cls, "messageType");
        zzfhz.zzc(zzfjvVarZzk, "schema");
        zzfjv<T> zzfjvVar2 = (zzfjv) this.zzpro.putIfAbsent(cls, zzfjvVarZzk);
        return zzfjvVar2 != null ? zzfjvVar2 : zzfjvVarZzk;
    }
}
