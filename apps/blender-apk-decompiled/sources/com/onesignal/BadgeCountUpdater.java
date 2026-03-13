package com.onesignal;

import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalDbContract;
import com.onesignal.shortcutbadger.ShortcutBadger;

/* JADX INFO: loaded from: classes.dex */
class BadgeCountUpdater {
    private static int badgesEnabled = -1;

    BadgeCountUpdater() {
    }

    private static boolean areBadgeSettingsEnabled(Context context) {
        if (badgesEnabled != -1) {
            return badgesEnabled == 1;
        }
        try {
            Bundle bundle = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128).metaData;
            if (bundle != null) {
                badgesEnabled = !"DISABLE".equals(bundle.getString("com.onesignal.BadgeCount")) ? 1 : 0;
            } else {
                badgesEnabled = 1;
            }
        } catch (Throwable th) {
            badgesEnabled = 0;
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error reading meta-data tag 'com.onesignal.BadgeCount'. Disabling badge setting.", th);
        }
        return badgesEnabled == 1;
    }

    private static boolean areBadgesEnabled(Context context) {
        return areBadgeSettingsEnabled(context) && OSUtils.areNotificationsEnabled(context);
    }

    static void update(SQLiteDatabase sQLiteDatabase, Context context) {
        if (areBadgesEnabled(context)) {
            if (Build.VERSION.SDK_INT >= 23) {
                updateStandard(context);
            } else {
                updateFallback(sQLiteDatabase, context);
            }
        }
    }

    @RequiresApi(api = 23)
    private static void updateStandard(Context context) {
        int i = 0;
        for (StatusBarNotification statusBarNotification : ((NotificationManager) context.getSystemService(OneSignalDbContract.NotificationTable.TABLE_NAME)).getActiveNotifications()) {
            if (!NotificationLimitManager.isGroupSummary(statusBarNotification)) {
                i++;
            }
        }
        updateCount(i, context);
    }

    private static void updateFallback(SQLiteDatabase sQLiteDatabase, Context context) {
        Cursor cursorQuery = sQLiteDatabase.query(OneSignalDbContract.NotificationTable.TABLE_NAME, null, OneSignalDbHelper.recentUninteractedWithNotificationsWhere().toString(), null, null, null, null, NotificationLimitManager.MAX_NUMBER_OF_NOTIFICATIONS_STR);
        int count = cursorQuery.getCount();
        cursorQuery.close();
        updateCount(count, context);
    }

    static void updateCount(int i, Context context) {
        if (areBadgeSettingsEnabled(context)) {
            try {
                ShortcutBadger.applyCountOrThrow(context, i);
            } catch (Throwable unused) {
            }
        }
    }
}
