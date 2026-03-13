package com.flurry.sdk;

/* JADX INFO: loaded from: classes.dex */
public enum ik {
    COMPLETE(1),
    TIMEOUT(2),
    INVALID_RESPONSE(3),
    PENDING_COMPLETION(4);

    int e;

    ik(int i) {
        this.e = i;
    }

    public static ik a(int i) {
        switch (i) {
            case 1:
                return COMPLETE;
            case 2:
                return TIMEOUT;
            case 3:
                return INVALID_RESPONSE;
            case 4:
                return PENDING_COMPLETION;
            default:
                return null;
        }
    }
}
