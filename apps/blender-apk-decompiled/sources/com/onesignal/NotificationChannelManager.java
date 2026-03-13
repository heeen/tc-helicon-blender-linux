package com.onesignal;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalDbContract;
import java.util.HashSet;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class NotificationChannelManager {
    private static final String DEFAULT_CHANNEL_ID = "fcm_fallback_notification_channel";
    private static final String RESTORE_CHANNEL_ID = "restored_OS_notifications";

    private static int priorityToImportance(int i) {
        if (i > 9) {
            return 5;
        }
        if (i > 7) {
            return 4;
        }
        if (i > 5) {
            return 3;
        }
        if (i > 3) {
            return 2;
        }
        return i > 1 ? 1 : 0;
    }

    NotificationChannelManager() {
    }

    static String createNotificationChannel(NotificationGenerationJob notificationGenerationJob) {
        if (Build.VERSION.SDK_INT < 26) {
            return DEFAULT_CHANNEL_ID;
        }
        Context context = notificationGenerationJob.context;
        JSONObject jSONObject = notificationGenerationJob.jsonPayload;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(OneSignalDbContract.NotificationTable.TABLE_NAME);
        if (notificationGenerationJob.restoring) {
            return createRestoreChannel(notificationManager);
        }
        if (jSONObject.has("oth_chnl")) {
            String strOptString = jSONObject.optString("oth_chnl");
            if (notificationManager.getNotificationChannel(strOptString) != null) {
                return strOptString;
            }
        }
        if (!jSONObject.has("chnl")) {
            return createDefaultChannel(notificationManager);
        }
        try {
            return createChannel(context, notificationManager, jSONObject);
        } catch (JSONException e) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Could not create notification channel due to JSON payload error!", e);
            return DEFAULT_CHANNEL_ID;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:14:0x0044  */
    @android.support.annotation.RequiresApi(api = 26)
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static java.lang.String createChannel(android.content.Context r6, android.app.NotificationManager r7, org.json.JSONObject r8) throws org.json.JSONException {
        /*
            Method dump skipped, instruction units count: 292
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.onesignal.NotificationChannelManager.createChannel(android.content.Context, android.app.NotificationManager, org.json.JSONObject):java.lang.String");
    }

    @RequiresApi(api = 26)
    private static String createDefaultChannel(NotificationManager notificationManager) {
        NotificationChannel notificationChannel = new NotificationChannel(DEFAULT_CHANNEL_ID, "Miscellaneous", 3);
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);
        return DEFAULT_CHANNEL_ID;
    }

    @RequiresApi(api = 26)
    private static String createRestoreChannel(NotificationManager notificationManager) {
        notificationManager.createNotificationChannel(new NotificationChannel(RESTORE_CHANNEL_ID, "Restored", 2));
        return RESTORE_CHANNEL_ID;
    }

    static void processChannelList(@NonNull Context context, @Nullable JSONArray jSONArray) {
        if (Build.VERSION.SDK_INT >= 26 && jSONArray != null) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(OneSignalDbContract.NotificationTable.TABLE_NAME);
            HashSet hashSet = new HashSet();
            int length = jSONArray.length();
            for (int i = 0; i < length; i++) {
                try {
                    hashSet.add(createChannel(context, notificationManager, jSONArray.getJSONObject(i)));
                } catch (JSONException e) {
                    OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Could not create notification channel due to JSON payload error!", e);
                }
            }
            Iterator<NotificationChannel> it = notificationManager.getNotificationChannels().iterator();
            while (it.hasNext()) {
                String id = it.next().getId();
                if (id.startsWith("OS_") && !hashSet.contains(id)) {
                    notificationManager.deleteNotificationChannel(id);
                }
            }
        }
    }
}
