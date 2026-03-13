package com.google.android.gms.internal;

/* JADX INFO: loaded from: classes.dex */
public enum zzfld {
    INT(0),
    LONG(0L),
    FLOAT(Float.valueOf(0.0f)),
    DOUBLE(Double.valueOf(0.0d)),
    BOOLEAN(false),
    STRING(""),
    BYTE_STRING(zzfgs.zzpnw),
    ENUM(null),
    MESSAGE(null);

    private final Object zzpvc;

    zzfld(Object obj) {
        this.zzpvc = obj;
    }
}
