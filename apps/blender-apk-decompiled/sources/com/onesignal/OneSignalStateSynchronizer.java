package com.onesignal;

import com.onesignal.LocationGMS;
import com.onesignal.OneSignal;
import com.onesignal.UserStateSynchronizer;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class OneSignalStateSynchronizer {
    private static UserStateEmailSynchronizer userStateEmailSynchronizer;
    private static UserStatePushSynchronizer userStatePushSynchronizer;

    OneSignalStateSynchronizer() {
    }

    static UserStatePushSynchronizer getPushStateSynchronizer() {
        if (userStatePushSynchronizer == null) {
            userStatePushSynchronizer = new UserStatePushSynchronizer();
        }
        return userStatePushSynchronizer;
    }

    static UserStateEmailSynchronizer getEmailStateSynchronizer() {
        if (userStateEmailSynchronizer == null) {
            userStateEmailSynchronizer = new UserStateEmailSynchronizer();
        }
        return userStateEmailSynchronizer;
    }

    static boolean persist() {
        boolean zPersist = getPushStateSynchronizer().persist();
        boolean zPersist2 = getEmailStateSynchronizer().persist();
        if (zPersist2) {
            zPersist2 = getEmailStateSynchronizer().getRegistrationId() != null;
        }
        return zPersist || zPersist2;
    }

    static void clearLocation() {
        getPushStateSynchronizer().clearLocation();
        getEmailStateSynchronizer().clearLocation();
    }

    static void initUserState() {
        getPushStateSynchronizer().initUserState();
        getEmailStateSynchronizer().initUserState();
    }

    static void syncUserState(boolean z) {
        getPushStateSynchronizer().syncUserState(z);
        getEmailStateSynchronizer().syncUserState(z);
    }

    static void sendTags(JSONObject jSONObject, OneSignal.ChangeTagsUpdateHandler changeTagsUpdateHandler) {
        try {
            JSONObject jSONObjectPut = new JSONObject().put("tags", jSONObject);
            getPushStateSynchronizer().sendTags(jSONObjectPut, changeTagsUpdateHandler);
            getEmailStateSynchronizer().sendTags(jSONObjectPut, changeTagsUpdateHandler);
        } catch (JSONException e) {
            changeTagsUpdateHandler.onFailure(new OneSignal.SendTagsError(-1, "Encountered an error attempting to serialize your tags into JSON: " + e.getMessage() + "\n" + e.getStackTrace()));
            e.printStackTrace();
        }
    }

    static void syncHashedEmail(String str) {
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("em_m", OSUtils.hexDigest(str, "MD5"));
            jSONObject.put("em_s", OSUtils.hexDigest(str, "SHA-1"));
            getPushStateSynchronizer().syncHashedEmail(jSONObject);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    static void setEmail(String str, String str2) {
        getPushStateSynchronizer().setEmail(str, str2);
        getEmailStateSynchronizer().setEmail(str, str2);
    }

    static void setSubscription(boolean z) {
        getPushStateSynchronizer().setSubscription(z);
    }

    static boolean getUserSubscribePreference() {
        return getPushStateSynchronizer().getUserSubscribePreference();
    }

    static void setPermission(boolean z) {
        getPushStateSynchronizer().setPermission(z);
    }

    static void updateLocation(LocationGMS.LocationPoint locationPoint) {
        getPushStateSynchronizer().updateLocation(locationPoint);
        getEmailStateSynchronizer().updateLocation(locationPoint);
    }

    static boolean getSubscribed() {
        return getPushStateSynchronizer().getSubscribed();
    }

    static String getRegistrationId() {
        return getPushStateSynchronizer().getRegistrationId();
    }

    static UserStateSynchronizer.GetTagsResult getTags(boolean z) {
        return getPushStateSynchronizer().getTags(z);
    }

    static void resetCurrentState() {
        getPushStateSynchronizer().resetCurrentState();
        getEmailStateSynchronizer().resetCurrentState();
        OneSignal.saveUserId(null);
        OneSignal.saveEmailId(null);
        OneSignal.setLastSessionTime(-3660L);
    }

    static void updateDeviceInfo(JSONObject jSONObject) {
        getPushStateSynchronizer().updateDeviceInfo(jSONObject);
        getEmailStateSynchronizer().updateDeviceInfo(jSONObject);
    }

    static void updatePushState(JSONObject jSONObject) {
        getPushStateSynchronizer().updateState(jSONObject);
    }

    static void refreshEmailState() {
        getEmailStateSynchronizer().refresh();
    }

    static void setNewSession() {
        getPushStateSynchronizer().setNewSession();
        getEmailStateSynchronizer().setNewSession();
    }

    static boolean getSyncAsNewSession() {
        return getPushStateSynchronizer().getSyncAsNewSession() || getEmailStateSynchronizer().getSyncAsNewSession();
    }

    static void setNewSessionForEmail() {
        getEmailStateSynchronizer().setNewSession();
    }

    static void logoutEmail() {
        getPushStateSynchronizer().logoutEmail();
        getEmailStateSynchronizer().logoutEmail();
    }

    static void setExternalUserId(String str) throws JSONException {
        getPushStateSynchronizer().setExternalUserId(str);
        getEmailStateSynchronizer().setExternalUserId(str);
    }

    static void readyToUpdate(boolean z) {
        getPushStateSynchronizer().readyToUpdate(z);
        getEmailStateSynchronizer().readyToUpdate(z);
    }
}
