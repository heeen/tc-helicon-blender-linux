package com.onesignal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/* JADX INFO: loaded from: classes.dex */
public class UpgradeReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT == 24) {
            return;
        }
        NotificationRestorer.startDelayedRestoreTaskFromReceiver(context);
    }
}
