package com.google.firebase.messaging;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.WorkerThread;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.firebase.iid.zzz;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

/* JADX INFO: loaded from: classes.dex */
public class FirebaseMessagingService extends com.google.firebase.iid.zzb {
    private static final Queue<String> zzoma = new ArrayDeque(10);

    static boolean zzal(Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        return "1".equals(bundle.getString("google.c.a.e"));
    }

    static void zzr(Bundle bundle) {
        Iterator<String> it = bundle.keySet().iterator();
        while (it.hasNext()) {
            String next = it.next();
            if (next != null && next.startsWith("google.c.")) {
                it.remove();
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x002e  */
    /* JADX WARN: Removed duplicated region for block: B:68:0x00f7  */
    /* JADX WARN: Removed duplicated region for block: B:70:0x00fb  */
    /* JADX WARN: Removed duplicated region for block: B:73:0x010f  */
    /* JADX WARN: Removed duplicated region for block: B:77:0x012c  */
    /* JADX WARN: Removed duplicated region for block: B:78:0x0136  */
    /* JADX WARN: Removed duplicated region for block: B:79:0x013a  */
    @Override // com.google.firebase.iid.zzb
    @com.google.android.gms.common.internal.Hide
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final void handleIntent(android.content.Intent r10) {
        /*
            Method dump skipped, instruction units count: 438
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.firebase.messaging.FirebaseMessagingService.handleIntent(android.content.Intent):void");
    }

    @WorkerThread
    public void onDeletedMessages() {
    }

    @WorkerThread
    public void onMessageReceived(RemoteMessage remoteMessage) {
    }

    @WorkerThread
    public void onMessageSent(String str) {
    }

    @WorkerThread
    public void onSendError(String str, Exception exc) {
    }

    @Override // com.google.firebase.iid.zzb
    @Hide
    protected final Intent zzp(Intent intent) {
        return zzz.zzclq().zzclr();
    }

    @Override // com.google.firebase.iid.zzb
    @Hide
    public final boolean zzq(Intent intent) {
        if (!"com.google.firebase.messaging.NOTIFICATION_OPEN".equals(intent.getAction())) {
            return false;
        }
        PendingIntent pendingIntent = (PendingIntent) intent.getParcelableExtra("pending_intent");
        if (pendingIntent != null) {
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException unused) {
                Log.e("FirebaseMessaging", "Notification pending intent canceled");
            }
        }
        if (!zzal(intent.getExtras())) {
            return true;
        }
        zzd.zzg(this, intent);
        return true;
    }
}
