package com.onesignal;

import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class OSSubscriptionState implements Cloneable {
    private boolean accepted;
    OSObservable<Object, OSSubscriptionState> observable = new OSObservable<>("changed", false);
    private String pushToken;
    private String userId;
    private boolean userSubscriptionSetting;

    OSSubscriptionState(boolean z, boolean z2) {
        if (z) {
            this.userSubscriptionSetting = OneSignalPrefs.getBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_SUBSCRIPTION_LAST, false);
            this.userId = OneSignalPrefs.getString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_PLAYER_ID_LAST, null);
            this.pushToken = OneSignalPrefs.getString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_PUSH_TOKEN_LAST, null);
            this.accepted = OneSignalPrefs.getBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_PERMISSION_ACCEPTED_LAST, false);
            return;
        }
        this.userSubscriptionSetting = OneSignalStateSynchronizer.getUserSubscribePreference();
        this.userId = OneSignal.getUserId();
        this.pushToken = OneSignalStateSynchronizer.getRegistrationId();
        this.accepted = z2;
    }

    void changed(OSPermissionState oSPermissionState) {
        setAccepted(oSPermissionState.getEnabled());
    }

    void setUserId(String str) {
        boolean z = !str.equals(this.userId);
        this.userId = str;
        if (z) {
            this.observable.notifyChange(this);
        }
    }

    public String getUserId() {
        return this.userId;
    }

    void setPushToken(String str) {
        if (str == null) {
            return;
        }
        boolean z = !str.equals(this.pushToken);
        this.pushToken = str;
        if (z) {
            this.observable.notifyChange(this);
        }
    }

    public String getPushToken() {
        return this.pushToken;
    }

    void setUserSubscriptionSetting(boolean z) {
        boolean z2 = this.userSubscriptionSetting != z;
        this.userSubscriptionSetting = z;
        if (z2) {
            this.observable.notifyChange(this);
        }
    }

    public boolean getUserSubscriptionSetting() {
        return this.userSubscriptionSetting;
    }

    private void setAccepted(boolean z) {
        boolean subscribed = getSubscribed();
        this.accepted = z;
        if (subscribed != getSubscribed()) {
            this.observable.notifyChange(this);
        }
    }

    public boolean getSubscribed() {
        return this.userId != null && this.pushToken != null && this.userSubscriptionSetting && this.accepted;
    }

    void persistAsFrom() {
        OneSignalPrefs.saveBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_SUBSCRIPTION_LAST, this.userSubscriptionSetting);
        OneSignalPrefs.saveString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_PLAYER_ID_LAST, this.userId);
        OneSignalPrefs.saveString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_PUSH_TOKEN_LAST, this.pushToken);
        OneSignalPrefs.saveBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_PERMISSION_ACCEPTED_LAST, this.accepted);
    }

    boolean compare(OSSubscriptionState oSSubscriptionState) {
        if (this.userSubscriptionSetting == oSSubscriptionState.userSubscriptionSetting) {
            if ((this.userId != null ? this.userId : "").equals(oSSubscriptionState.userId != null ? oSSubscriptionState.userId : "")) {
                if ((this.pushToken != null ? this.pushToken : "").equals(oSSubscriptionState.pushToken != null ? oSSubscriptionState.pushToken : "") && this.accepted == oSSubscriptionState.accepted) {
                    return false;
                }
            }
        }
        return true;
    }

    protected Object clone() {
        try {
            return super.clone();
        } catch (Throwable unused) {
            return null;
        }
    }

    public JSONObject toJSONObject() {
        JSONObject jSONObject = new JSONObject();
        try {
            if (this.userId != null) {
                jSONObject.put("userId", this.userId);
            } else {
                jSONObject.put("userId", JSONObject.NULL);
            }
            if (this.pushToken != null) {
                jSONObject.put("pushToken", this.pushToken);
            } else {
                jSONObject.put("pushToken", JSONObject.NULL);
            }
            jSONObject.put("userSubscriptionSetting", this.userSubscriptionSetting);
            jSONObject.put("subscribed", getSubscribed());
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return jSONObject;
    }

    public String toString() {
        return toJSONObject().toString();
    }
}
