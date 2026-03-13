package com.onesignal;

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalDbContract;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class NotificationSummaryManager {
    NotificationSummaryManager() {
    }

    static void updatePossibleDependentSummaryOnDismiss(Context context, SQLiteDatabase sQLiteDatabase, int i) {
        Cursor cursorQuery = sQLiteDatabase.query(OneSignalDbContract.NotificationTable.TABLE_NAME, new String[]{OneSignalDbContract.NotificationTable.COLUMN_NAME_GROUP_ID}, "android_notification_id = " + i, null, null, null, null);
        if (cursorQuery.moveToFirst()) {
            String string = cursorQuery.getString(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_GROUP_ID));
            cursorQuery.close();
            if (string != null) {
                updateSummaryNotificationAfterChildRemoved(context, sQLiteDatabase, string, true);
                return;
            }
            return;
        }
        cursorQuery.close();
    }

    static void updateSummaryNotificationAfterChildRemoved(Context context, SQLiteDatabase sQLiteDatabase, String str, boolean z) {
        try {
            Cursor cursorInternalUpdateSummaryNotificationAfterChildRemoved = internalUpdateSummaryNotificationAfterChildRemoved(context, sQLiteDatabase, str, z);
            if (cursorInternalUpdateSummaryNotificationAfterChildRemoved != null && !cursorInternalUpdateSummaryNotificationAfterChildRemoved.isClosed()) {
                cursorInternalUpdateSummaryNotificationAfterChildRemoved.close();
            }
        } catch (Throwable th) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error running updateSummaryNotificationAfterChildRemoved!", th);
        }
    }

    private static Cursor internalUpdateSummaryNotificationAfterChildRemoved(Context context, SQLiteDatabase sQLiteDatabase, String str, boolean z) throws Throwable {
        Cursor cursorQuery = sQLiteDatabase.query(OneSignalDbContract.NotificationTable.TABLE_NAME, new String[]{OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID, OneSignalDbContract.NotificationTable.COLUMN_NAME_CREATED_TIME}, "group_id = ? AND dismissed = 0 AND opened = 0 AND is_summary = 0", new String[]{str}, null, null, "_id DESC");
        int count = cursorQuery.getCount();
        if (count == 0) {
            cursorQuery.close();
            Integer summaryNotificationId = getSummaryNotificationId(sQLiteDatabase, str);
            if (summaryNotificationId == null) {
                return cursorQuery;
            }
            ((NotificationManager) context.getSystemService(OneSignalDbContract.NotificationTable.TABLE_NAME)).cancel(summaryNotificationId.intValue());
            ContentValues contentValues = new ContentValues();
            contentValues.put(z ? OneSignalDbContract.NotificationTable.COLUMN_NAME_DISMISSED : OneSignalDbContract.NotificationTable.COLUMN_NAME_OPENED, (Integer) 1);
            sQLiteDatabase.update(OneSignalDbContract.NotificationTable.TABLE_NAME, contentValues, "android_notification_id = " + summaryNotificationId, null);
            return cursorQuery;
        }
        if (count == 1) {
            cursorQuery.close();
            if (getSummaryNotificationId(sQLiteDatabase, str) == null) {
                return cursorQuery;
            }
            restoreSummary(context, str);
            return cursorQuery;
        }
        try {
            cursorQuery.moveToFirst();
            Long lValueOf = Long.valueOf(cursorQuery.getLong(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_CREATED_TIME)));
            cursorQuery.close();
            if (getSummaryNotificationId(sQLiteDatabase, str) == null) {
                return cursorQuery;
            }
            NotificationGenerationJob notificationGenerationJob = new NotificationGenerationJob(context);
            notificationGenerationJob.restoring = true;
            notificationGenerationJob.shownTimeStamp = lValueOf;
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("grp", str);
            notificationGenerationJob.jsonPayload = jSONObject;
            GenerateNotification.updateSummaryNotification(notificationGenerationJob);
        } catch (JSONException unused) {
        }
        return cursorQuery;
    }

    private static void restoreSummary(Context context, String str) throws Throwable {
        Cursor cursorQuery;
        String[] strArr = {str};
        Cursor cursor = null;
        try {
            try {
                cursorQuery = OneSignalDbHelper.getInstance(context).getReadableDbWithRetries().query(OneSignalDbContract.NotificationTable.TABLE_NAME, NotificationRestorer.COLUMNS_FOR_RESTORE, "group_id = ? AND dismissed = 0 AND opened = 0 AND is_summary = 0", strArr, null, null, null);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            NotificationRestorer.showNotificationsFromCursor(context, cursorQuery, 0);
            if (cursorQuery != null && !cursorQuery.isClosed()) {
                cursorQuery.close();
            }
        } catch (Throwable th3) {
            th = th3;
            cursor = cursorQuery;
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            throw th;
        }
    }

    private static Integer getSummaryNotificationId(SQLiteDatabase sQLiteDatabase, String str) throws Throwable {
        Integer num;
        Cursor cursor = null;
        try {
            try {
                Cursor cursorQuery = sQLiteDatabase.query(OneSignalDbContract.NotificationTable.TABLE_NAME, new String[]{OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID}, "group_id = ? AND dismissed = 0 AND opened = 0 AND is_summary = 1", new String[]{str}, null, null, null);
                try {
                    try {
                        if (!cursorQuery.moveToFirst()) {
                            cursorQuery.close();
                            if (cursorQuery != null && !cursorQuery.isClosed()) {
                                cursorQuery.close();
                            }
                            return null;
                        }
                        Integer numValueOf = Integer.valueOf(cursorQuery.getInt(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID)));
                        try {
                            cursorQuery.close();
                            if (cursorQuery != null && !cursorQuery.isClosed()) {
                                cursorQuery.close();
                            }
                            return numValueOf;
                        } catch (Throwable th) {
                            cursor = cursorQuery;
                            num = numValueOf;
                            th = th;
                            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error getting android notification id for summary notification group: " + str, th);
                            if (cursor == null || cursor.isClosed()) {
                                return num;
                            }
                            cursor.close();
                            return num;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        cursor = cursorQuery;
                        if (cursor != null && !cursor.isClosed()) {
                            cursor.close();
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    cursor = cursorQuery;
                    num = null;
                }
            } catch (Throwable th4) {
                th = th4;
                num = null;
            }
        } catch (Throwable th5) {
            th = th5;
        }
    }
}
