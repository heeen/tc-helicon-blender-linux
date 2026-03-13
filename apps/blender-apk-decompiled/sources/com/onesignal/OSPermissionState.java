package com.onesignal;

import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class OSPermissionState implements Cloneable {
    private boolean enabled;
    OSObservable<Object, OSPermissionState> observable = new OSObservable<>("changed", false);

    OSPermissionState(boolean z) {
        if (z) {
            this.enabled = OneSignalPrefs.getBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_ACCEPTED_NOTIFICATION_LAST, false);
        } else {
            refreshAsTo();
        }
    }

    void refreshAsTo() {
        setEnabled(OSUtils.areNotificationsEnabled(OneSignal.appContext));
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    private void setEnabled(boolean z) {
        boolean z2 = this.enabled != z;
        this.enabled = z;
        if (z2) {
            this.observable.notifyChange(this);
        }
    }

    void persistAsFrom() {
        OneSignalPrefs.saveBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_ACCEPTED_NOTIFICATION_LAST, this.enabled);
    }

    boolean compare(OSPermissionState oSPermissionState) {
        return this.enabled != oSPermissionState.enabled;
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
            jSONObject.put("enabled", this.enabled);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return jSONObject;
    }

    public String toString() {
        return toJSONObject().toString();
    }
}
