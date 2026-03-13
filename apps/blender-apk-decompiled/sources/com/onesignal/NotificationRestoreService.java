package com.onesignal;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/* JADX INFO: loaded from: classes.dex */
public class NotificationRestoreService extends IntentService {
    public NotificationRestoreService() {
        super("NotificationRestoreService");
    }

    @Override // android.app.IntentService
    protected void onHandleIntent(Intent intent) throws Throwable {
        if (intent == null) {
            return;
        }
        Thread.currentThread().setPriority(10);
        OneSignal.setAppContext(this);
        NotificationRestorer.restore(this);
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }
}
