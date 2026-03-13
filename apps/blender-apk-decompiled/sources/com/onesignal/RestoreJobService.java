package com.onesignal;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/* JADX INFO: loaded from: classes.dex */
public class RestoreJobService extends JobIntentService {
    static final int RESTORE_SERVICE_JOB_ID = 2071862122;

    @Override // com.onesignal.JobIntentService
    public /* bridge */ /* synthetic */ boolean isStopped() {
        return super.isStopped();
    }

    @Override // com.onesignal.JobIntentService, android.app.Service
    public /* bridge */ /* synthetic */ IBinder onBind(@NonNull Intent intent) {
        return super.onBind(intent);
    }

    @Override // com.onesignal.JobIntentService, android.app.Service
    public /* bridge */ /* synthetic */ void onCreate() {
        super.onCreate();
    }

    @Override // com.onesignal.JobIntentService, android.app.Service
    public /* bridge */ /* synthetic */ void onDestroy() {
        super.onDestroy();
    }

    @Override // com.onesignal.JobIntentService, android.app.Service
    public /* bridge */ /* synthetic */ int onStartCommand(@Nullable Intent intent, int i, int i2) {
        return super.onStartCommand(intent, i, i2);
    }

    @Override // com.onesignal.JobIntentService
    public /* bridge */ /* synthetic */ boolean onStopCurrentWork() {
        return super.onStopCurrentWork();
    }

    @Override // com.onesignal.JobIntentService
    public /* bridge */ /* synthetic */ void setInterruptIfStopped(boolean z) {
        super.setInterruptIfStopped(z);
    }

    @Override // com.onesignal.JobIntentService
    protected final void onHandleWork(Intent intent) throws Throwable {
        Bundle extras;
        if (intent == null || (extras = intent.getExtras()) == null) {
            return;
        }
        NotificationBundleProcessor.ProcessFromGCMIntentService(getApplicationContext(), new BundleCompatBundle(extras), null);
    }
}
