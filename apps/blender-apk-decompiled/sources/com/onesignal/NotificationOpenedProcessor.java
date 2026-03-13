package com.onesignal;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.NotificationManagerCompat;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalDbContract;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class NotificationOpenedProcessor {
    NotificationOpenedProcessor() {
    }

    static void processFromContext(Context context, Intent intent) throws Throwable {
        if (isOneSignalIntent(intent)) {
            OneSignal.setAppContext(context);
            handleDismissFromActionButtonPress(context, intent);
            processIntent(context, intent);
        }
    }

    private static boolean isOneSignalIntent(Intent intent) {
        return intent.hasExtra("onesignal_data") || intent.hasExtra("summary") || intent.hasExtra("notificationId");
    }

    private static void handleDismissFromActionButtonPress(Context context, Intent intent) {
        if (intent.getBooleanExtra("action_button", false)) {
            NotificationManagerCompat.from(context).cancel(intent.getIntExtra("notificationId", 0));
            context.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:38:0x008f  */
    /* JADX WARN: Removed duplicated region for block: B:58:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r3v0 */
    /* JADX WARN: Type inference failed for: r3v1, types: [com.onesignal.OneSignal$LOG_LEVEL] */
    /* JADX WARN: Type inference failed for: r3v10 */
    /* JADX WARN: Type inference failed for: r3v2 */
    /* JADX WARN: Type inference failed for: r3v3 */
    /* JADX WARN: Type inference failed for: r3v4, types: [android.database.sqlite.SQLiteDatabase] */
    /* JADX WARN: Type inference failed for: r3v5 */
    /* JADX WARN: Type inference failed for: r3v6 */
    /* JADX WARN: Type inference failed for: r3v7 */
    /* JADX WARN: Type inference failed for: r3v8 */
    /* JADX WARN: Type inference failed for: r3v9 */
    /* JADX WARN: Type inference failed for: r5v11, types: [android.database.sqlite.SQLiteDatabase] */
    /* JADX WARN: Type inference failed for: r5v8 */
    /* JADX WARN: Type inference failed for: r5v9, types: [android.database.sqlite.SQLiteDatabase] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    static void processIntent(android.content.Context r7, android.content.Intent r8) throws java.lang.Throwable {
        /*
            java.lang.String r0 = "summary"
            java.lang.String r0 = r8.getStringExtra(r0)
            java.lang.String r1 = "dismissed"
            r2 = 0
            boolean r1 = r8.getBooleanExtra(r1, r2)
            r3 = 0
            if (r1 != 0) goto L43
            org.json.JSONObject r4 = new org.json.JSONObject     // Catch: java.lang.Throwable -> L3f
            java.lang.String r5 = "onesignal_data"
            java.lang.String r5 = r8.getStringExtra(r5)     // Catch: java.lang.Throwable -> L3f
            r4.<init>(r5)     // Catch: java.lang.Throwable -> L3f
            java.lang.String r5 = "notificationId"
            java.lang.String r6 = "notificationId"
            int r6 = r8.getIntExtra(r6, r2)     // Catch: java.lang.Throwable -> L3f
            r4.put(r5, r6)     // Catch: java.lang.Throwable -> L3f
            java.lang.String r5 = "onesignal_data"
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> L3f
            r8.putExtra(r5, r4)     // Catch: java.lang.Throwable -> L3f
            org.json.JSONObject r4 = new org.json.JSONObject     // Catch: java.lang.Throwable -> L3f
            java.lang.String r5 = "onesignal_data"
            java.lang.String r5 = r8.getStringExtra(r5)     // Catch: java.lang.Throwable -> L3f
            r4.<init>(r5)     // Catch: java.lang.Throwable -> L3f
            org.json.JSONArray r4 = com.onesignal.NotificationBundleProcessor.newJsonArray(r4)     // Catch: java.lang.Throwable -> L3f
            goto L44
        L3f:
            r4 = move-exception
            r4.printStackTrace()
        L43:
            r4 = r3
        L44:
            com.onesignal.OneSignalDbHelper r5 = com.onesignal.OneSignalDbHelper.getInstance(r7)
            android.database.sqlite.SQLiteDatabase r5 = r5.getWritableDbWithRetries()     // Catch: java.lang.Throwable -> L74 java.lang.Exception -> L77
            r5.beginTransaction()     // Catch: java.lang.Throwable -> L6f java.lang.Exception -> L71
            if (r1 != 0) goto L56
            if (r0 == 0) goto L56
            addChildNotifications(r4, r0, r5)     // Catch: java.lang.Throwable -> L6f java.lang.Exception -> L71
        L56:
            markNotificationsConsumed(r7, r8, r5)     // Catch: java.lang.Throwable -> L6f java.lang.Exception -> L71
            if (r0 != 0) goto L66
            java.lang.String r0 = "grp"
            java.lang.String r0 = r8.getStringExtra(r0)     // Catch: java.lang.Throwable -> L6f java.lang.Exception -> L71
            if (r0 == 0) goto L66
            com.onesignal.NotificationSummaryManager.updateSummaryNotificationAfterChildRemoved(r7, r5, r0, r1)     // Catch: java.lang.Throwable -> L6f java.lang.Exception -> L71
        L66:
            r5.setTransactionSuccessful()     // Catch: java.lang.Throwable -> L6f java.lang.Exception -> L71
            if (r5 == 0) goto L8d
            r5.endTransaction()     // Catch: java.lang.Throwable -> L85
            goto L8d
        L6f:
            r7 = move-exception
            goto L99
        L71:
            r0 = move-exception
            r3 = r5
            goto L78
        L74:
            r7 = move-exception
            r5 = r3
            goto L99
        L77:
            r0 = move-exception
        L78:
            com.onesignal.OneSignal$LOG_LEVEL r5 = com.onesignal.OneSignal.LOG_LEVEL.ERROR     // Catch: java.lang.Throwable -> L74
            java.lang.String r6 = "Error processing notification open or dismiss record! "
            com.onesignal.OneSignal.Log(r5, r6, r0)     // Catch: java.lang.Throwable -> L74
            if (r3 == 0) goto L8d
            r3.endTransaction()     // Catch: java.lang.Throwable -> L85
            goto L8d
        L85:
            r0 = move-exception
            com.onesignal.OneSignal$LOG_LEVEL r3 = com.onesignal.OneSignal.LOG_LEVEL.ERROR
            java.lang.String r5 = "Error closing transaction! "
            com.onesignal.OneSignal.Log(r3, r5, r0)
        L8d:
            if (r1 != 0) goto L98
            java.lang.String r0 = "from_alert"
            boolean r8 = r8.getBooleanExtra(r0, r2)
            com.onesignal.OneSignal.handleNotificationOpen(r7, r4, r8)
        L98:
            return
        L99:
            if (r5 == 0) goto La7
            r5.endTransaction()     // Catch: java.lang.Throwable -> L9f
            goto La7
        L9f:
            r8 = move-exception
            com.onesignal.OneSignal$LOG_LEVEL r0 = com.onesignal.OneSignal.LOG_LEVEL.ERROR
            java.lang.String r1 = "Error closing transaction! "
            com.onesignal.OneSignal.Log(r0, r1, r8)
        La7:
            throw r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.onesignal.NotificationOpenedProcessor.processIntent(android.content.Context, android.content.Intent):void");
    }

    private static void addChildNotifications(JSONArray jSONArray, String str, SQLiteDatabase sQLiteDatabase) {
        Cursor cursorQuery = sQLiteDatabase.query(OneSignalDbContract.NotificationTable.TABLE_NAME, new String[]{OneSignalDbContract.NotificationTable.COLUMN_NAME_FULL_DATA}, "group_id = ? AND dismissed = 0 AND opened = 0 AND is_summary = 0", new String[]{str}, null, null, null);
        if (cursorQuery.getCount() > 1) {
            cursorQuery.moveToFirst();
            do {
                try {
                    jSONArray.put(new JSONObject(cursorQuery.getString(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_FULL_DATA))));
                } catch (Throwable unused) {
                    OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Could not parse JSON of sub notification in group: " + str);
                }
            } while (cursorQuery.moveToNext());
        }
        cursorQuery.close();
    }

    private static void markNotificationsConsumed(Context context, Intent intent, SQLiteDatabase sQLiteDatabase) {
        String str;
        String[] strArr;
        String stringExtra = intent.getStringExtra("summary");
        if (stringExtra != null) {
            str = "group_id = ?";
            strArr = new String[]{stringExtra};
        } else {
            str = "android_notification_id = " + intent.getIntExtra("notificationId", 0);
            strArr = null;
        }
        sQLiteDatabase.update(OneSignalDbContract.NotificationTable.TABLE_NAME, newContentValuesWithConsumed(intent), str, strArr);
        BadgeCountUpdater.update(sQLiteDatabase, context);
    }

    private static ContentValues newContentValuesWithConsumed(Intent intent) {
        ContentValues contentValues = new ContentValues();
        if (intent.getBooleanExtra(OneSignalDbContract.NotificationTable.COLUMN_NAME_DISMISSED, false)) {
            contentValues.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_DISMISSED, (Integer) 1);
        } else {
            contentValues.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_OPENED, (Integer) 1);
        }
        return contentValues;
    }
}
