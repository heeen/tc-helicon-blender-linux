package com.onesignal;

import android.content.Context;
import android.net.Uri;
import com.onesignal.NotificationExtenderService;
import com.onesignal.OneSignalDbContract;
import java.security.SecureRandom;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class NotificationGenerationJob {
    Context context;
    JSONObject jsonPayload;
    Integer orgFlags;
    Uri orgSound;
    CharSequence overriddenBodyFromExtender;
    Integer overriddenFlags;
    Uri overriddenSound;
    CharSequence overriddenTitleFromExtender;
    NotificationExtenderService.OverrideSettings overrideSettings;
    boolean restoring;
    boolean showAsAlert;
    Long shownTimeStamp;

    NotificationGenerationJob(Context context) {
        this.context = context;
    }

    CharSequence getTitle() {
        if (this.overriddenTitleFromExtender != null) {
            return this.overriddenTitleFromExtender;
        }
        return this.jsonPayload.optString(OneSignalDbContract.NotificationTable.COLUMN_NAME_TITLE, null);
    }

    CharSequence getBody() {
        if (this.overriddenBodyFromExtender != null) {
            return this.overriddenBodyFromExtender;
        }
        return this.jsonPayload.optString("alert", null);
    }

    Integer getAndroidId() {
        if (this.overrideSettings == null) {
            this.overrideSettings = new NotificationExtenderService.OverrideSettings();
        }
        if (this.overrideSettings.androidNotificationId == null) {
            this.overrideSettings.androidNotificationId = Integer.valueOf(new SecureRandom().nextInt());
        }
        return this.overrideSettings.androidNotificationId;
    }

    int getAndroidIdWithoutCreate() {
        if (this.overrideSettings == null || this.overrideSettings.androidNotificationId == null) {
            return -1;
        }
        return this.overrideSettings.androidNotificationId.intValue();
    }

    void setAndroidIdWithOutOverriding(Integer num) {
        if (num == null) {
            return;
        }
        if (this.overrideSettings == null || this.overrideSettings.androidNotificationId == null) {
            if (this.overrideSettings == null) {
                this.overrideSettings = new NotificationExtenderService.OverrideSettings();
            }
            this.overrideSettings.androidNotificationId = num;
        }
    }

    boolean hasExtender() {
        return (this.overrideSettings == null || this.overrideSettings.extender == null) ? false : true;
    }
}
