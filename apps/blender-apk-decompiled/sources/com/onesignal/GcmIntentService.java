package com.onesignal;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

/* JADX INFO: loaded from: classes.dex */
public class GcmIntentService extends IntentService {
    public GcmIntentService() {
        super("GcmIntentService");
        setIntentRedelivery(true);
    }

    @Override // android.app.IntentService
    protected void onHandleIntent(Intent intent) throws Throwable {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        NotificationBundleProcessor.ProcessFromGCMIntentService(this, new BundleCompatBundle(extras), null);
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
}
