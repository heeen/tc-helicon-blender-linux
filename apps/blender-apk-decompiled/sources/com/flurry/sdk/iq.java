package com.flurry.sdk;

/* JADX INFO: loaded from: classes.dex */
public enum iq {
    INSTALL(1),
    SESSION_START(2),
    SESSION_END(3),
    APPLICATION_EVENT(4);

    int e;

    iq(int i) {
        this.e = i;
    }

    public static iq a(int i) {
        switch (i) {
            case 1:
                return INSTALL;
            case 2:
                return SESSION_START;
            case 3:
                return SESSION_END;
            case 4:
                return APPLICATION_EVENT;
            default:
                return null;
        }
    }
}
