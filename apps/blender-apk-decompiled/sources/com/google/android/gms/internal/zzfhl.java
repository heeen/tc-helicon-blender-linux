package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
final class zzfhl {
    private static Class<?> zzpot = zzczc();

    private static Class<?> zzczc() {
        try {
            return Class.forName("com.google.protobuf.ExtensionRegistry");
        } catch (ClassNotFoundException unused) {
            return null;
        }
    }

    public static zzfhm zzczd() {
        if (zzpot != null) {
            try {
                return zzty("getEmptyRegistry");
            } catch (Exception unused) {
            }
        }
        return zzfhm.zzpow;
    }

    private static final zzfhm zzty(String str) throws Exception {
        return (zzfhm) zzpot.getDeclaredMethod(str, new Class[0]).invoke(null, new Object[0]);
    }
}
