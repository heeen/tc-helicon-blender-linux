package com.onesignal;

import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalDbContract;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/* JADX INFO: loaded from: classes.dex */
class NotificationLimitManager {
    private static final int MAX_NUMBER_OF_NOTIFICATIONS_INT = 49;
    static final String MAX_NUMBER_OF_NOTIFICATIONS_STR = Integer.toString(49);

    private static int getMaxNumberOfNotificationsInt() {
        return 49;
    }

    NotificationLimitManager() {
    }

    private static String getMaxNumberOfNotificationsString() {
        return MAX_NUMBER_OF_NOTIFICATIONS_STR;
    }

    static void clearOldestOverLimit(Context context, int i) throws Throwable {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                clearOldestOverLimitStandard(context, i);
            } else {
                clearOldestOverLimitFallback(context, i);
            }
        } catch (Throwable unused) {
            clearOldestOverLimitFallback(context, i);
        }
    }

    @RequiresApi(api = 23)
    static void clearOldestOverLimitStandard(Context context, int i) throws Throwable {
        StatusBarNotification[] activeNotifications = ((NotificationManager) context.getSystemService(OneSignalDbContract.NotificationTable.TABLE_NAME)).getActiveNotifications();
        int length = (activeNotifications.length - getMaxNumberOfNotificationsInt()) + i;
        if (length < 1) {
            return;
        }
        TreeMap treeMap = new TreeMap();
        for (StatusBarNotification statusBarNotification : activeNotifications) {
            if (!isGroupSummary(statusBarNotification)) {
                treeMap.put(Long.valueOf(statusBarNotification.getNotification().when), Integer.valueOf(statusBarNotification.getId()));
            }
        }
        Iterator it = treeMap.entrySet().iterator();
        while (it.hasNext()) {
            OneSignal.cancelNotification(((Integer) ((Map.Entry) it.next()).getValue()).intValue());
            length--;
            if (length <= 0) {
                return;
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v0 */
    /* JADX WARN: Type inference failed for: r0v1 */
    /* JADX WARN: Type inference failed for: r0v10, types: [int] */
    /* JADX WARN: Type inference failed for: r0v11 */
    /* JADX WARN: Type inference failed for: r0v12, types: [int] */
    /* JADX WARN: Type inference failed for: r0v13 */
    /* JADX WARN: Type inference failed for: r0v2, types: [android.database.Cursor] */
    /* JADX WARN: Type inference failed for: r0v3, types: [android.database.Cursor] */
    /* JADX WARN: Type inference failed for: r0v4 */
    /* JADX WARN: Type inference failed for: r0v5 */
    /* JADX WARN: Type inference failed for: r0v6 */
    /* JADX WARN: Type inference failed for: r0v9, types: [int] */
    static void clearOldestOverLimitFallback(Context context, int i) throws Throwable {
        Cursor cursorQuery;
        ?? count = 0;
        count = 0;
        try {
            try {
                cursorQuery = OneSignalDbHelper.getInstance(context).getReadableDbWithRetries().query(OneSignalDbContract.NotificationTable.TABLE_NAME, new String[]{OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID}, OneSignalDbHelper.recentUninteractedWithNotificationsWhere().toString(), null, null, null, "_id", getMaxNumberOfNotificationsString() + i);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            count = (cursorQuery.getCount() - getMaxNumberOfNotificationsInt()) + i;
        } catch (Throwable th3) {
            th = th3;
            count = cursorQuery;
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error clearing oldest notifications over limit! ", th);
            if (count != 0 && !count.isClosed()) {
                count.close();
            }
        }
        if (count < 1) {
            if (cursorQuery == null || cursorQuery.isClosed()) {
                return;
            }
            cursorQuery.close();
            return;
        }
        while (cursorQuery.moveToNext()) {
            OneSignal.cancelNotification(cursorQuery.getInt(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID)));
            count--;
            if (count <= 0) {
                break;
            }
        }
        if (cursorQuery != null && !cursorQuery.isClosed()) {
            cursorQuery.close();
        }
    }

    @RequiresApi(api = 21)
    static boolean isGroupSummary(StatusBarNotification statusBarNotification) {
        return (statusBarNotification.getNotification().flags & 512) != 0;
    }
}
