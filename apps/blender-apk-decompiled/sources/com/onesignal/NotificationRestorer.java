package com.onesignal;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalDbContract;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
class NotificationRestorer {
    static final String[] COLUMNS_FOR_RESTORE = {OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID, OneSignalDbContract.NotificationTable.COLUMN_NAME_FULL_DATA, OneSignalDbContract.NotificationTable.COLUMN_NAME_CREATED_TIME};
    static final int DEFAULT_TTL_IF_NOT_IN_PAYLOAD = 259200;
    private static final int DELAY_BETWEEN_NOTIFICATION_RESTORES_MS = 200;
    private static final int RESTORE_KICKOFF_REQUEST_CODE = 2071862120;
    private static final int RESTORE_NOTIFICATIONS_DELAY_MS = 15000;
    public static boolean restored;

    NotificationRestorer() {
    }

    static void asyncRestore(final Context context) {
        new Thread(new Runnable() { // from class: com.onesignal.NotificationRestorer.1
            @Override // java.lang.Runnable
            public void run() throws Throwable {
                Thread.currentThread().setPriority(10);
                NotificationRestorer.restore(context);
            }
        }, "OS_RESTORE_NOTIFS").start();
    }

    @WorkerThread
    public static void restore(Context context) throws Throwable {
        if (OSUtils.areNotificationsEnabled(context) && !restored) {
            restored = true;
            OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "Restoring notifications");
            OneSignalDbHelper oneSignalDbHelper = OneSignalDbHelper.getInstance(context);
            deleteOldNotificationsFromDb(oneSignalDbHelper);
            StringBuilder sbRecentUninteractedWithNotificationsWhere = OneSignalDbHelper.recentUninteractedWithNotificationsWhere();
            skipVisibleNotifications(context, sbRecentUninteractedWithNotificationsWhere);
            queryAndRestoreNotificationsAndBadgeCount(context, oneSignalDbHelper, sbRecentUninteractedWithNotificationsWhere);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:28:0x0039 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static void deleteOldNotificationsFromDb(com.onesignal.OneSignalDbHelper r4) throws java.lang.Throwable {
        /*
            r0 = 0
            android.database.sqlite.SQLiteDatabase r4 = r4.getWritableDbWithRetries()     // Catch: java.lang.Throwable -> L21 java.lang.Throwable -> L26
            r4.beginTransaction()     // Catch: java.lang.Throwable -> L1d java.lang.Throwable -> L1f
            com.onesignal.NotificationBundleProcessor.deleteOldNotifications(r4)     // Catch: java.lang.Throwable -> L1d java.lang.Throwable -> L1f
            r4.setTransactionSuccessful()     // Catch: java.lang.Throwable -> L1d java.lang.Throwable -> L1f
            if (r4 == 0) goto L36
            r4.endTransaction()     // Catch: java.lang.Throwable -> L14
            goto L36
        L14:
            r4 = move-exception
            com.onesignal.OneSignal$LOG_LEVEL r0 = com.onesignal.OneSignal.LOG_LEVEL.ERROR
            java.lang.String r1 = "Error closing transaction! "
            com.onesignal.OneSignal.Log(r0, r1, r4)
            goto L36
        L1d:
            r0 = move-exception
            goto L37
        L1f:
            r0 = move-exception
            goto L2a
        L21:
            r4 = move-exception
            r3 = r0
            r0 = r4
            r4 = r3
            goto L37
        L26:
            r4 = move-exception
            r3 = r0
            r0 = r4
            r4 = r3
        L2a:
            com.onesignal.OneSignal$LOG_LEVEL r1 = com.onesignal.OneSignal.LOG_LEVEL.ERROR     // Catch: java.lang.Throwable -> L1d
            java.lang.String r2 = "Error deleting old notification records! "
            com.onesignal.OneSignal.Log(r1, r2, r0)     // Catch: java.lang.Throwable -> L1d
            if (r4 == 0) goto L36
            r4.endTransaction()     // Catch: java.lang.Throwable -> L14
        L36:
            return
        L37:
            if (r4 == 0) goto L45
            r4.endTransaction()     // Catch: java.lang.Throwable -> L3d
            goto L45
        L3d:
            r4 = move-exception
            com.onesignal.OneSignal$LOG_LEVEL r1 = com.onesignal.OneSignal.LOG_LEVEL.ERROR
            java.lang.String r2 = "Error closing transaction! "
            com.onesignal.OneSignal.Log(r1, r2, r4)
        L45:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.onesignal.NotificationRestorer.deleteOldNotificationsFromDb(com.onesignal.OneSignalDbHelper):void");
    }

    private static void queryAndRestoreNotificationsAndBadgeCount(Context context, OneSignalDbHelper oneSignalDbHelper, StringBuilder sb) throws Throwable {
        SQLiteDatabase readableDbWithRetries;
        Cursor cursorQuery;
        OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "Querying DB for notifs to restore: " + sb.toString());
        Cursor cursor = null;
        try {
            try {
                readableDbWithRetries = oneSignalDbHelper.getReadableDbWithRetries();
                cursorQuery = readableDbWithRetries.query(OneSignalDbContract.NotificationTable.TABLE_NAME, COLUMNS_FOR_RESTORE, sb.toString(), null, null, null, "_id DESC", NotificationLimitManager.MAX_NUMBER_OF_NOTIFICATIONS_STR);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            showNotificationsFromCursor(context, cursorQuery, 200);
            BadgeCountUpdater.update(readableDbWithRetries, context);
            if (cursorQuery == null || cursorQuery.isClosed()) {
                return;
            }
            cursorQuery.close();
        } catch (Throwable th3) {
            th = th3;
            cursor = cursorQuery;
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            throw th;
        }
    }

    private static void skipVisibleNotifications(Context context, StringBuilder sb) {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        try {
            StatusBarNotification[] activeNotifications = ((NotificationManager) context.getSystemService(OneSignalDbContract.NotificationTable.TABLE_NAME)).getActiveNotifications();
            if (activeNotifications.length == 0) {
                return;
            }
            ArrayList arrayList = new ArrayList();
            for (StatusBarNotification statusBarNotification : activeNotifications) {
                arrayList.add(Integer.valueOf(statusBarNotification.getId()));
            }
            sb.append(" AND android_notification_id NOT IN (");
            sb.append(TextUtils.join(",", arrayList));
            sb.append(")");
        } catch (Throwable unused) {
        }
    }

    static void showNotificationsFromCursor(Context context, Cursor cursor, int i) {
        if (cursor.moveToFirst()) {
            boolean z = NotificationExtenderService.getIntent(context) != null;
            do {
                if (z) {
                    Intent intent = NotificationExtenderService.getIntent(context);
                    addRestoreExtras(intent, cursor);
                    NotificationExtenderService.enqueueWork(context, intent.getComponent(), 2071862121, intent, false);
                } else {
                    RestoreJobService.enqueueWork(context, new ComponentName(context, (Class<?>) RestoreJobService.class), 2071862122, addRestoreExtras(new Intent(), cursor), false);
                }
                if (i > 0) {
                    OSUtils.sleep(i);
                }
            } while (cursor.moveToNext());
        }
    }

    private static Intent addRestoreExtras(Intent intent, Cursor cursor) {
        int i = cursor.getInt(cursor.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID));
        String string = cursor.getString(cursor.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_FULL_DATA));
        intent.putExtra("json_payload", string).putExtra("android_notif_id", i).putExtra("restoring", true).putExtra("timestamp", Long.valueOf(cursor.getLong(cursor.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_CREATED_TIME))));
        return intent;
    }

    static void startDelayedRestoreTaskFromReceiver(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "scheduleRestoreKickoffJob");
            ((JobScheduler) context.getSystemService("jobscheduler")).schedule(new JobInfo.Builder(RESTORE_KICKOFF_REQUEST_CODE, new ComponentName(context, (Class<?>) RestoreKickoffJobService.class)).setOverrideDeadline(15000L).setMinimumLatency(15000L).build());
        } else {
            OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "scheduleRestoreKickoffAlarmTask");
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(context.getPackageName(), NotificationRestoreService.class.getName()));
            PendingIntent service = PendingIntent.getService(context, RESTORE_KICKOFF_REQUEST_CODE, intent, 268435456);
            ((AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM)).set(1, System.currentTimeMillis() + 15000, service);
        }
    }
}
