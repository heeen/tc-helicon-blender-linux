package com.google.firebase.messaging;

/* JADX INFO: loaded from: classes.dex */
public final class SendException extends Exception {
    public static final int ERROR_INVALID_PARAMETERS = 1;
    public static final int ERROR_SIZE = 2;
    public static final int ERROR_TOO_MANY_MESSAGES = 4;
    public static final int ERROR_TTL_EXCEEDED = 3;
    public static final int ERROR_UNKNOWN = 0;
    private final int mErrorCode;

    /* JADX WARN: Removed duplicated region for block: B:27:0x0054  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    SendException(java.lang.String r8) {
        /*
            r7 = this;
            r7.<init>(r8)
            r0 = 4
            r1 = 3
            r2 = 2
            r3 = 1
            r4 = 0
            if (r8 == 0) goto L54
            java.util.Locale r5 = java.util.Locale.US
            java.lang.String r8 = r8.toLowerCase(r5)
            r5 = -1
            int r6 = r8.hashCode()
            switch(r6) {
                case -1743242157: goto L41;
                case -1290953729: goto L37;
                case -920906446: goto L2d;
                case -617027085: goto L23;
                case -95047692: goto L19;
                default: goto L18;
            }
        L18:
            goto L4a
        L19:
            java.lang.String r6 = "missing_to"
            boolean r8 = r8.equals(r6)
            if (r8 == 0) goto L4a
            r5 = r3
            goto L4a
        L23:
            java.lang.String r6 = "messagetoobig"
            boolean r8 = r8.equals(r6)
            if (r8 == 0) goto L4a
            r5 = r2
            goto L4a
        L2d:
            java.lang.String r6 = "invalid_parameters"
            boolean r8 = r8.equals(r6)
            if (r8 == 0) goto L4a
            r5 = r4
            goto L4a
        L37:
            java.lang.String r6 = "toomanymessages"
            boolean r8 = r8.equals(r6)
            if (r8 == 0) goto L4a
            r5 = r0
            goto L4a
        L41:
            java.lang.String r6 = "service_not_available"
            boolean r8 = r8.equals(r6)
            if (r8 == 0) goto L4a
            r5 = r1
        L4a:
            switch(r5) {
                case 0: goto L52;
                case 1: goto L52;
                case 2: goto L50;
                case 3: goto L4e;
                case 4: goto L55;
                default: goto L4d;
            }
        L4d:
            goto L54
        L4e:
            r0 = r1
            goto L55
        L50:
            r0 = r2
            goto L55
        L52:
            r0 = r3
            goto L55
        L54:
            r0 = r4
        L55:
            r7.mErrorCode = r0
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.firebase.messaging.SendException.<init>(java.lang.String):void");
    }

    public final int getErrorCode() {
        return this.mErrorCode;
    }
}
