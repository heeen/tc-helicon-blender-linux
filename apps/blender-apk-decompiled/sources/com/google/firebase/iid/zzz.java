package com.google.firebase.iid;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.SimpleArrayMap;
import com.google.android.gms.common.internal.Hide;
import java.util.ArrayDeque;
import java.util.Queue;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzz {
    private static zzz zzolj;
    private final SimpleArrayMap<String, String> zzolk = new SimpleArrayMap<>();
    private Boolean zzoll = null;

    @VisibleForTesting
    final Queue<Intent> zzolm = new ArrayDeque();

    @VisibleForTesting
    private Queue<Intent> zzoln = new ArrayDeque();

    private zzz() {
    }

    public static PendingIntent zza(Context context, int i, Intent intent, int i2) {
        Intent intent2 = new Intent(context, (Class<?>) FirebaseInstanceIdReceiver.class);
        intent2.setAction("com.google.firebase.MESSAGING_EVENT");
        intent2.putExtra("wrapped_intent", intent);
        return PendingIntent.getBroadcast(context, i, intent2, 1073741824);
    }

    public static synchronized zzz zzclq() {
        if (zzolj == null) {
            zzolj = new zzz();
        }
        return zzolj;
    }

    /* JADX WARN: Removed duplicated region for block: B:36:0x00b4  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private final int zze(android.content.Context r7, android.content.Intent r8) {
        /*
            Method dump skipped, instruction units count: 326
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.firebase.iid.zzz.zze(android.content.Context, android.content.Intent):int");
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x0023  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final int zzb(android.content.Context r3, java.lang.String r4, android.content.Intent r5) {
        /*
            r2 = this;
            int r0 = r4.hashCode()
            r1 = -842411455(0xffffffffcdc9d241, float:-4.2324995E8)
            if (r0 == r1) goto L19
            r1 = 41532704(0x279bd20, float:1.8347907E-37)
            if (r0 == r1) goto Lf
            goto L23
        Lf:
            java.lang.String r0 = "com.google.firebase.MESSAGING_EVENT"
            boolean r0 = r4.equals(r0)
            if (r0 == 0) goto L23
            r0 = 1
            goto L24
        L19:
            java.lang.String r0 = "com.google.firebase.INSTANCE_ID_EVENT"
            boolean r0 = r4.equals(r0)
            if (r0 == 0) goto L23
            r0 = 0
            goto L24
        L23:
            r0 = -1
        L24:
            switch(r0) {
                case 0: goto L3d;
                case 1: goto L3a;
                default: goto L27;
            }
        L27:
            java.lang.String r2 = "FirebaseInstanceId"
            java.lang.String r3 = "Unknown service action: "
            java.lang.String r4 = java.lang.String.valueOf(r4)
            int r5 = r4.length()
            if (r5 == 0) goto L53
            java.lang.String r3 = r3.concat(r4)
            goto L59
        L3a:
            java.util.Queue<android.content.Intent> r0 = r2.zzoln
            goto L3f
        L3d:
            java.util.Queue<android.content.Intent> r0 = r2.zzolm
        L3f:
            r0.offer(r5)
            android.content.Intent r5 = new android.content.Intent
            r5.<init>(r4)
            java.lang.String r4 = r3.getPackageName()
            r5.setPackage(r4)
            int r2 = r2.zze(r3, r5)
            return r2
        L53:
            java.lang.String r4 = new java.lang.String
            r4.<init>(r3)
            r3 = r4
        L59:
            android.util.Log.w(r2, r3)
            r2 = 500(0x1f4, float:7.0E-43)
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.firebase.iid.zzz.zzb(android.content.Context, java.lang.String, android.content.Intent):int");
    }

    public final Intent zzclr() {
        return this.zzoln.poll();
    }
}
