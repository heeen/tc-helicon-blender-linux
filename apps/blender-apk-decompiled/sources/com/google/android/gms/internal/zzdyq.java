package com.google.android.gms.internal;

import java.io.PrintStream;
import java.io.PrintWriter;

/* JADX INFO: loaded from: classes.dex */
public final class zzdyq {
    private static zzdyr zzmmf;
    private static int zzmmg;

    static final class zza extends zzdyr {
        zza() {
        }

        @Override // com.google.android.gms.internal.zzdyr
        public final void zza(Throwable th, PrintStream printStream) {
            th.printStackTrace(printStream);
        }

        @Override // com.google.android.gms.internal.zzdyr
        public final void zza(Throwable th, PrintWriter printWriter) {
            th.printStackTrace(printWriter);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x0017 A[Catch: Throwable -> 0x0015, TryCatch #1 {Throwable -> 0x0015, blocks: (B:5:0x0007, B:7:0x000f, B:10:0x0017, B:12:0x0020, B:13:0x0026), top: B:27:0x0007 }] */
    static {
        /*
            r0 = 1
            java.lang.Integer r1 = zzbss()     // Catch: java.lang.Throwable -> L2c
            if (r1 == 0) goto L17
            int r2 = r1.intValue()     // Catch: java.lang.Throwable -> L15
            r3 = 19
            if (r2 < r3) goto L17
            com.google.android.gms.internal.zzdyv r2 = new com.google.android.gms.internal.zzdyv     // Catch: java.lang.Throwable -> L15
            r2.<init>()     // Catch: java.lang.Throwable -> L15
            goto L63
        L15:
            r2 = move-exception
            goto L2e
        L17:
            java.lang.String r2 = "com.google.devtools.build.android.desugar.runtime.twr_disable_mimic"
            boolean r2 = java.lang.Boolean.getBoolean(r2)     // Catch: java.lang.Throwable -> L15
            r2 = r2 ^ r0
            if (r2 == 0) goto L26
            com.google.android.gms.internal.zzdyu r2 = new com.google.android.gms.internal.zzdyu     // Catch: java.lang.Throwable -> L15
            r2.<init>()     // Catch: java.lang.Throwable -> L15
            goto L63
        L26:
            com.google.android.gms.internal.zzdyq$zza r2 = new com.google.android.gms.internal.zzdyq$zza     // Catch: java.lang.Throwable -> L15
            r2.<init>()     // Catch: java.lang.Throwable -> L15
            goto L63
        L2c:
            r2 = move-exception
            r1 = 0
        L2e:
            java.io.PrintStream r3 = java.lang.System.err
            java.lang.Class<com.google.android.gms.internal.zzdyq$zza> r4 = com.google.android.gms.internal.zzdyq.zza.class
            java.lang.String r4 = r4.getName()
            java.lang.String r5 = java.lang.String.valueOf(r4)
            int r5 = r5.length()
            int r5 = r5 + 132
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>(r5)
            java.lang.String r5 = "An error has occured when initializing the try-with-resources desuguring strategy. The default strategy "
            r6.append(r5)
            r6.append(r4)
            java.lang.String r4 = "will be used. The error is: "
            r6.append(r4)
            java.lang.String r4 = r6.toString()
            r3.println(r4)
            java.io.PrintStream r3 = java.lang.System.err
            r2.printStackTrace(r3)
            com.google.android.gms.internal.zzdyq$zza r2 = new com.google.android.gms.internal.zzdyq$zza
            r2.<init>()
        L63:
            com.google.android.gms.internal.zzdyq.zzmmf = r2
            if (r1 != 0) goto L68
            goto L6c
        L68:
            int r0 = r1.intValue()
        L6c:
            com.google.android.gms.internal.zzdyq.zzmmg = r0
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.internal.zzdyq.<clinit>():void");
    }

    public static void zza(Throwable th, PrintStream printStream) {
        zzmmf.zza(th, printStream);
    }

    public static void zza(Throwable th, PrintWriter printWriter) {
        zzmmf.zza(th, printWriter);
    }

    private static Integer zzbss() {
        try {
            return (Integer) Class.forName("android.os.Build$VERSION").getField("SDK_INT").get(null);
        } catch (Exception e) {
            System.err.println("Failed to retrieve value from android.os.Build$VERSION.SDK_INT due to the following exception.");
            e.printStackTrace(System.err);
            return null;
        }
    }
}
