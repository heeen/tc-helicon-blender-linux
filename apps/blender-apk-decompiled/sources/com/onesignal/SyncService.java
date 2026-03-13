package com.onesignal;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.onesignal.OneSignalSyncServiceUtils;

/* JADX INFO: loaded from: classes.dex */
public class SyncService extends Service {
    @Override // android.app.Service
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        OneSignalSyncServiceUtils.doBackgroundSync(this, new OneSignalSyncServiceUtils.LegacySyncRunnable(this));
        return 1;
    }
}
