package com.onesignal.shortcutbadger.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class BroadcastHelper {
    public static boolean canResolveBroadcast(Context context, Intent intent) {
        List<ResolveInfo> listQueryBroadcastReceivers = context.getPackageManager().queryBroadcastReceivers(intent, 0);
        return listQueryBroadcastReceivers != null && listQueryBroadcastReceivers.size() > 0;
    }
}
