package com.onesignal;

import org.json.JSONException;

/* JADX INFO: loaded from: classes.dex */
class UserStatePush extends UserState {
    UserStatePush(String str, boolean z) {
        super(str, z);
    }

    @Override // com.onesignal.UserState
    UserState newInstance(String str) {
        return new UserStatePush(str, false);
    }

    @Override // com.onesignal.UserState
    protected void addDependFields() {
        try {
            this.syncValues.put("notification_types", getNotificationTypes());
        } catch (JSONException unused) {
        }
    }

    private int getNotificationTypes() {
        int iOptInt = this.dependValues.optInt("subscribableStatus", 1);
        if (iOptInt < -2) {
            return iOptInt;
        }
        if (this.dependValues.optBoolean("androidPermission", true)) {
            return !this.dependValues.optBoolean("userSubscribePref", true) ? -2 : 1;
        }
        return 0;
    }

    @Override // com.onesignal.UserState
    boolean isSubscribed() {
        return getNotificationTypes() > 0;
    }
}
