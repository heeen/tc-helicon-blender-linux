package com.onesignal;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/* JADX INFO: loaded from: classes.dex */
public class NotificationOpenedActivity extends Activity {
    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) throws Throwable {
        super.onCreate(bundle);
        NotificationOpenedProcessor.processFromContext(this, getIntent());
        finish();
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) throws Throwable {
        super.onNewIntent(intent);
        NotificationOpenedProcessor.processFromContext(this, getIntent());
        finish();
    }
}
