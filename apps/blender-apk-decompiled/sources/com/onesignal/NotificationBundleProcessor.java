package com.onesignal;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import com.onesignal.NotificationExtenderService;
import com.onesignal.OSNotificationPayload;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalDbContract;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class NotificationBundleProcessor {
    static final String DEFAULT_ACTION = "__DEFAULT__";

    NotificationBundleProcessor() {
    }

    static void ProcessFromGCMIntentService(Context context, BundleCompat bundleCompat, NotificationExtenderService.OverrideSettings overrideSettings) throws Throwable {
        OneSignal.setAppContext(context);
        try {
            String string = bundleCompat.getString("json_payload");
            if (string == null) {
                OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "json_payload key is nonexistent from mBundle passed to ProcessFromGCMIntentService: " + bundleCompat);
                return;
            }
            NotificationGenerationJob notificationGenerationJob = new NotificationGenerationJob(context);
            notificationGenerationJob.restoring = bundleCompat.getBoolean("restoring", false);
            notificationGenerationJob.shownTimeStamp = bundleCompat.getLong("timestamp");
            notificationGenerationJob.jsonPayload = new JSONObject(string);
            if (notificationGenerationJob.restoring || !OneSignal.notValidOrDuplicated(context, notificationGenerationJob.jsonPayload)) {
                if (bundleCompat.containsKey("android_notif_id")) {
                    if (overrideSettings == null) {
                        overrideSettings = new NotificationExtenderService.OverrideSettings();
                    }
                    overrideSettings.androidNotificationId = bundleCompat.getInt("android_notif_id");
                }
                notificationGenerationJob.overrideSettings = overrideSettings;
                ProcessJobForDisplay(notificationGenerationJob);
                if (notificationGenerationJob.restoring) {
                    OSUtils.sleep(100);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static int ProcessJobForDisplay(NotificationGenerationJob notificationGenerationJob) throws Throwable {
        notificationGenerationJob.showAsAlert = OneSignal.getInAppAlertNotificationEnabled() && OneSignal.isAppActive();
        processCollapseKey(notificationGenerationJob);
        if (notificationGenerationJob.hasExtender() || shouldDisplay(notificationGenerationJob.jsonPayload.optString("alert"))) {
            GenerateNotification.fromJsonPayload(notificationGenerationJob);
        }
        if (!notificationGenerationJob.restoring) {
            saveNotification(notificationGenerationJob, false);
            try {
                JSONObject jSONObject = new JSONObject(notificationGenerationJob.jsonPayload.toString());
                jSONObject.put("notificationId", notificationGenerationJob.getAndroidId());
                OneSignal.handleNotificationReceived(newJsonArray(jSONObject), true, notificationGenerationJob.showAsAlert);
            } catch (Throwable unused) {
            }
        }
        return notificationGenerationJob.getAndroidId().intValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static JSONArray bundleAsJsonArray(Bundle bundle) {
        JSONArray jSONArray = new JSONArray();
        jSONArray.put(bundleAsJSONObject(bundle));
        return jSONArray;
    }

    private static void saveNotification(Context context, Bundle bundle, boolean z, int i) throws Throwable {
        NotificationGenerationJob notificationGenerationJob = new NotificationGenerationJob(context);
        notificationGenerationJob.jsonPayload = bundleAsJSONObject(bundle);
        notificationGenerationJob.overrideSettings = new NotificationExtenderService.OverrideSettings();
        notificationGenerationJob.overrideSettings.androidNotificationId = Integer.valueOf(i);
        saveNotification(notificationGenerationJob, z);
    }

    static void saveNotification(NotificationGenerationJob notificationGenerationJob, boolean z) throws Throwable {
        SQLiteDatabase writableDbWithRetries;
        OneSignal.LOG_LEVEL log_level;
        String str;
        Context context = notificationGenerationJob.context;
        JSONObject jSONObject = notificationGenerationJob.jsonPayload;
        try {
            JSONObject jSONObject2 = new JSONObject(notificationGenerationJob.jsonPayload.optString("custom"));
            SQLiteDatabase sQLiteDatabase = null;
            try {
                try {
                    writableDbWithRetries = OneSignalDbHelper.getInstance(notificationGenerationJob.context).getWritableDbWithRetries();
                } catch (Throwable th) {
                    th = th;
                    writableDbWithRetries = sQLiteDatabase;
                }
            } catch (Exception e) {
                e = e;
            }
            try {
                writableDbWithRetries.beginTransaction();
                deleteOldNotifications(writableDbWithRetries);
                if (notificationGenerationJob.getAndroidIdWithoutCreate() != -1) {
                    String str2 = "android_notification_id = " + notificationGenerationJob.getAndroidIdWithoutCreate();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_DISMISSED, (Integer) 1);
                    writableDbWithRetries.update(OneSignalDbContract.NotificationTable.TABLE_NAME, contentValues, str2, null);
                    BadgeCountUpdater.update(writableDbWithRetries, context);
                }
                ContentValues contentValues2 = new ContentValues();
                contentValues2.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_NOTIFICATION_ID, jSONObject2.optString("i"));
                if (jSONObject.has("grp")) {
                    contentValues2.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_GROUP_ID, jSONObject.optString("grp"));
                }
                if (jSONObject.has("collapse_key") && !"do_not_collapse".equals(jSONObject.optString("collapse_key"))) {
                    contentValues2.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_COLLAPSE_ID, jSONObject.optString("collapse_key"));
                }
                contentValues2.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_OPENED, Integer.valueOf(z ? 1 : 0));
                if (!z) {
                    contentValues2.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID, Integer.valueOf(notificationGenerationJob.getAndroidIdWithoutCreate()));
                }
                if (notificationGenerationJob.getTitle() != null) {
                    contentValues2.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_TITLE, notificationGenerationJob.getTitle().toString());
                }
                if (notificationGenerationJob.getBody() != null) {
                    contentValues2.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_MESSAGE, notificationGenerationJob.getBody().toString());
                }
                contentValues2.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_EXPIRE_TIME, Long.valueOf((jSONObject.optLong("google.sent_time", SystemClock.currentThreadTimeMillis()) / 1000) + ((long) jSONObject.optInt("google.ttl", 259200))));
                contentValues2.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_FULL_DATA, jSONObject.toString());
                writableDbWithRetries.insertOrThrow(OneSignalDbContract.NotificationTable.TABLE_NAME, null, contentValues2);
                if (!z) {
                    BadgeCountUpdater.update(writableDbWithRetries, context);
                }
                writableDbWithRetries.setTransactionSuccessful();
                if (writableDbWithRetries != null) {
                    try {
                        writableDbWithRetries.endTransaction();
                    } catch (Throwable th2) {
                        th = th2;
                        log_level = OneSignal.LOG_LEVEL.ERROR;
                        str = "Error closing transaction! ";
                        OneSignal.Log(log_level, str, th);
                    }
                }
            } catch (Exception e2) {
                e = e2;
                sQLiteDatabase = writableDbWithRetries;
                OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error saving notification record! ", e);
                if (sQLiteDatabase != null) {
                    try {
                        sQLiteDatabase.endTransaction();
                    } catch (Throwable th3) {
                        th = th3;
                        log_level = OneSignal.LOG_LEVEL.ERROR;
                        str = "Error closing transaction! ";
                        OneSignal.Log(log_level, str, th);
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                if (writableDbWithRetries != null) {
                    try {
                        writableDbWithRetries.endTransaction();
                    } catch (Throwable th5) {
                        OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error closing transaction! ", th5);
                    }
                }
                throw th;
            }
        } catch (JSONException e3) {
            e3.printStackTrace();
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:23:0x0064 -> B:31:0x006b). Please report as a decompilation issue!!! */
    static void markRestoredNotificationAsDismissed(NotificationGenerationJob notificationGenerationJob) throws Throwable {
        SQLiteDatabase writableDbWithRetries;
        if (notificationGenerationJob.getAndroidIdWithoutCreate() == -1) {
            return;
        }
        String str = "android_notification_id = " + notificationGenerationJob.getAndroidIdWithoutCreate();
        SQLiteDatabase sQLiteDatabase = null;
        try {
            try {
                try {
                    writableDbWithRetries = OneSignalDbHelper.getInstance(notificationGenerationJob.context).getWritableDbWithRetries();
                } catch (Throwable th) {
                    th = th;
                    writableDbWithRetries = sQLiteDatabase;
                }
            } catch (Exception e) {
                e = e;
            }
            try {
                writableDbWithRetries.beginTransaction();
                ContentValues contentValues = new ContentValues();
                contentValues.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_DISMISSED, (Integer) 1);
                writableDbWithRetries.update(OneSignalDbContract.NotificationTable.TABLE_NAME, contentValues, str, null);
                BadgeCountUpdater.update(writableDbWithRetries, notificationGenerationJob.context);
                writableDbWithRetries.setTransactionSuccessful();
                if (writableDbWithRetries != null) {
                    writableDbWithRetries.endTransaction();
                }
            } catch (Exception e2) {
                e = e2;
                sQLiteDatabase = writableDbWithRetries;
                OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error saving notification record! ", e);
                if (sQLiteDatabase != null) {
                    sQLiteDatabase.endTransaction();
                }
            } catch (Throwable th2) {
                th = th2;
                if (writableDbWithRetries != null) {
                    try {
                        writableDbWithRetries.endTransaction();
                    } catch (Throwable th3) {
                        OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error closing transaction! ", th3);
                    }
                }
                throw th;
            }
        } catch (Throwable th4) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error closing transaction! ", th4);
        }
    }

    static void deleteOldNotifications(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.delete(OneSignalDbContract.NotificationTable.TABLE_NAME, "created_time < " + ((System.currentTimeMillis() / 1000) - 604800), null);
    }

    static JSONObject bundleAsJSONObject(Bundle bundle) {
        JSONObject jSONObject = new JSONObject();
        for (String str : bundle.keySet()) {
            try {
                jSONObject.put(str, bundle.get(str));
            } catch (JSONException e) {
                OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "bundleAsJSONObject error for key: " + str, e);
            }
        }
        return jSONObject;
    }

    private static void unMinifyBundle(Bundle bundle) {
        JSONObject jSONObject;
        String string;
        if (bundle.containsKey("o")) {
            try {
                JSONObject jSONObject2 = new JSONObject(bundle.getString("custom"));
                if (jSONObject2.has("a")) {
                    jSONObject = jSONObject2.getJSONObject("a");
                } else {
                    jSONObject = new JSONObject();
                }
                JSONArray jSONArray = new JSONArray(bundle.getString("o"));
                bundle.remove("o");
                for (int i = 0; i < jSONArray.length(); i++) {
                    JSONObject jSONObject3 = jSONArray.getJSONObject(i);
                    String string2 = jSONObject3.getString("n");
                    jSONObject3.remove("n");
                    if (jSONObject3.has("i")) {
                        string = jSONObject3.getString("i");
                        jSONObject3.remove("i");
                    } else {
                        string = string2;
                    }
                    jSONObject3.put("id", string);
                    jSONObject3.put("text", string2);
                    if (jSONObject3.has("p")) {
                        jSONObject3.put("icon", jSONObject3.getString("p"));
                        jSONObject3.remove("p");
                    }
                }
                jSONObject.put("actionButtons", jSONArray);
                jSONObject.put("actionSelected", DEFAULT_ACTION);
                if (!jSONObject2.has("a")) {
                    jSONObject2.put("a", jSONObject);
                }
                bundle.putString("custom", jSONObject2.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    static OSNotificationPayload OSNotificationPayloadFrom(JSONObject jSONObject) {
        OSNotificationPayload oSNotificationPayload = new OSNotificationPayload();
        try {
            JSONObject jSONObject2 = new JSONObject(jSONObject.optString("custom"));
            oSNotificationPayload.notificationID = jSONObject2.optString("i");
            oSNotificationPayload.templateId = jSONObject2.optString("ti");
            oSNotificationPayload.templateName = jSONObject2.optString("tn");
            oSNotificationPayload.rawPayload = jSONObject.toString();
            oSNotificationPayload.additionalData = jSONObject2.optJSONObject("a");
            oSNotificationPayload.launchURL = jSONObject2.optString("u", null);
            oSNotificationPayload.body = jSONObject.optString("alert", null);
            oSNotificationPayload.title = jSONObject.optString(OneSignalDbContract.NotificationTable.COLUMN_NAME_TITLE, null);
            oSNotificationPayload.smallIcon = jSONObject.optString("sicon", null);
            oSNotificationPayload.bigPicture = jSONObject.optString("bicon", null);
            oSNotificationPayload.largeIcon = jSONObject.optString("licon", null);
            oSNotificationPayload.sound = jSONObject.optString("sound", null);
            oSNotificationPayload.groupKey = jSONObject.optString("grp", null);
            oSNotificationPayload.groupMessage = jSONObject.optString("grp_msg", null);
            oSNotificationPayload.smallIconAccentColor = jSONObject.optString("bgac", null);
            oSNotificationPayload.ledColor = jSONObject.optString("ledc", null);
            String strOptString = jSONObject.optString("vis", null);
            if (strOptString != null) {
                oSNotificationPayload.lockScreenVisibility = Integer.parseInt(strOptString);
            }
            oSNotificationPayload.fromProjectNumber = jSONObject.optString("from", null);
            oSNotificationPayload.priority = jSONObject.optInt("pri", 0);
            String strOptString2 = jSONObject.optString("collapse_key", null);
            if (!"do_not_collapse".equals(strOptString2)) {
                oSNotificationPayload.collapseId = strOptString2;
            }
            try {
                setActionButtons(oSNotificationPayload);
            } catch (Throwable th) {
                OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error assigning OSNotificationPayload.actionButtons values!", th);
            }
            try {
                setBackgroundImageLayout(oSNotificationPayload, jSONObject);
            } catch (Throwable th2) {
                OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error assigning OSNotificationPayload.backgroundImageLayout values!", th2);
            }
        } catch (Throwable th3) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error assigning OSNotificationPayload values!", th3);
        }
        return oSNotificationPayload;
    }

    private static void setActionButtons(OSNotificationPayload oSNotificationPayload) throws Throwable {
        if (oSNotificationPayload.additionalData == null || !oSNotificationPayload.additionalData.has("actionButtons")) {
            return;
        }
        JSONArray jSONArray = oSNotificationPayload.additionalData.getJSONArray("actionButtons");
        oSNotificationPayload.actionButtons = new ArrayList();
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObject = jSONArray.getJSONObject(i);
            OSNotificationPayload.ActionButton actionButton = new OSNotificationPayload.ActionButton();
            actionButton.id = jSONObject.optString("id", null);
            actionButton.text = jSONObject.optString("text", null);
            actionButton.icon = jSONObject.optString("icon", null);
            oSNotificationPayload.actionButtons.add(actionButton);
        }
        oSNotificationPayload.additionalData.remove("actionSelected");
        oSNotificationPayload.additionalData.remove("actionButtons");
    }

    private static void setBackgroundImageLayout(OSNotificationPayload oSNotificationPayload, JSONObject jSONObject) throws Throwable {
        String strOptString = jSONObject.optString("bg_img", null);
        if (strOptString != null) {
            JSONObject jSONObject2 = new JSONObject(strOptString);
            oSNotificationPayload.backgroundImageLayout = new OSNotificationPayload.BackgroundImageLayout();
            oSNotificationPayload.backgroundImageLayout.image = jSONObject2.optString("img");
            oSNotificationPayload.backgroundImageLayout.titleTextColor = jSONObject2.optString("tc");
            oSNotificationPayload.backgroundImageLayout.bodyTextColor = jSONObject2.optString("bc");
        }
    }

    private static void processCollapseKey(NotificationGenerationJob notificationGenerationJob) throws Throwable {
        Cursor cursorQuery;
        if (notificationGenerationJob.restoring || !notificationGenerationJob.jsonPayload.has("collapse_key") || "do_not_collapse".equals(notificationGenerationJob.jsonPayload.optString("collapse_key"))) {
            return;
        }
        Cursor cursor = null;
        try {
            try {
                cursorQuery = OneSignalDbHelper.getInstance(notificationGenerationJob.context).getReadableDbWithRetries().query(OneSignalDbContract.NotificationTable.TABLE_NAME, new String[]{OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID}, "collapse_id = ? AND dismissed = 0 AND opened = 0 ", new String[]{notificationGenerationJob.jsonPayload.optString("collapse_key")}, null, null, null);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            if (cursorQuery.moveToFirst()) {
                notificationGenerationJob.setAndroidIdWithOutOverriding(Integer.valueOf(cursorQuery.getInt(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID))));
            }
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

    static ProcessedBundleResult processBundleFromReceiver(Context context, final Bundle bundle) throws Throwable {
        ProcessedBundleResult processedBundleResult = new ProcessedBundleResult();
        if (OneSignal.getNotificationIdFromGCMBundle(bundle) == null) {
            return processedBundleResult;
        }
        processedBundleResult.isOneSignalPayload = true;
        unMinifyBundle(bundle);
        if (startExtenderService(context, bundle, processedBundleResult)) {
            return processedBundleResult;
        }
        processedBundleResult.isDup = OneSignal.notValidOrDuplicated(context, bundleAsJSONObject(bundle));
        if (!processedBundleResult.isDup && !shouldDisplay(bundle.getString("alert"))) {
            saveNotification(context, bundle, true, -1);
            new Thread(new Runnable() { // from class: com.onesignal.NotificationBundleProcessor.1
                @Override // java.lang.Runnable
                public void run() {
                    OneSignal.handleNotificationReceived(NotificationBundleProcessor.bundleAsJsonArray(bundle), false, false);
                }
            }, "OS_PROC_BUNDLE").start();
        }
        return processedBundleResult;
    }

    private static boolean startExtenderService(Context context, Bundle bundle, ProcessedBundleResult processedBundleResult) {
        Intent intent = NotificationExtenderService.getIntent(context);
        if (intent == null) {
            return false;
        }
        intent.putExtra("json_payload", bundleAsJSONObject(bundle).toString());
        intent.putExtra("timestamp", System.currentTimeMillis() / 1000);
        boolean z = Integer.parseInt(bundle.getString("pri", "0")) > 9;
        if (Build.VERSION.SDK_INT >= 21) {
            NotificationExtenderService.enqueueWork(context, intent.getComponent(), 2071862121, intent, z);
        } else {
            context.startService(intent);
        }
        processedBundleResult.hasExtenderService = true;
        return true;
    }

    static boolean shouldDisplay(String str) {
        return (str != null && !"".equals(str)) && (OneSignal.getNotificationsWhenActiveEnabled() || OneSignal.getInAppAlertNotificationEnabled() || !OneSignal.isAppActive());
    }

    static JSONArray newJsonArray(JSONObject jSONObject) {
        JSONArray jSONArray = new JSONArray();
        jSONArray.put(jSONObject);
        return jSONArray;
    }

    static boolean hasRemoteResource(Bundle bundle) {
        return isBuildKeyRemote(bundle, "licon") || isBuildKeyRemote(bundle, "bicon") || bundle.getString("bg_img", null) != null;
    }

    private static boolean isBuildKeyRemote(Bundle bundle, String str) {
        String strTrim = bundle.getString(str, "").trim();
        return strTrim.startsWith("http://") || strTrim.startsWith("https://");
    }

    static class ProcessedBundleResult {
        boolean hasExtenderService;
        boolean isDup;
        boolean isOneSignalPayload;

        ProcessedBundleResult() {
        }

        boolean processed() {
            return !this.isOneSignalPayload || this.hasExtenderService || this.isDup;
        }
    }
}
