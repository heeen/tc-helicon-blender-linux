package com.onesignal;

import com.onesignal.LocationGMS;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
abstract class UserState {
    static final int DEVICE_TYPE_ANDROID = 1;
    static final int DEVICE_TYPE_FIREOS = 2;
    static final int PUSH_STATUS_FIREBASE_FCM_ERROR_IOEXCEPTION = -11;
    static final int PUSH_STATUS_FIREBASE_FCM_ERROR_MISC_EXCEPTION = -12;
    static final int PUSH_STATUS_FIREBASE_FCM_ERROR_SERVICE_NOT_AVAILABLE = -9;
    static final int PUSH_STATUS_FIREBASE_FCM_INIT_ERROR = -8;
    static final int PUSH_STATUS_INVALID_FCM_SENDER_ID = -6;
    static final int PUSH_STATUS_MISSING_ANDROID_SUPPORT_LIBRARY = -3;
    static final int PUSH_STATUS_MISSING_FIREBASE_FCM_LIBRARY = -4;
    static final int PUSH_STATUS_NO_PERMISSION = 0;
    static final int PUSH_STATUS_OUTDATED_ANDROID_SUPPORT_LIBRARY = -5;
    static final int PUSH_STATUS_OUTDATED_GOOGLE_PLAY_SERVICES_APP = -7;
    static final int PUSH_STATUS_SUBSCRIBED = 1;
    static final int PUSH_STATUS_UNSUBSCRIBE = -2;
    JSONObject dependValues;
    private String persistKey;
    JSONObject syncValues;
    private static final String[] LOCATION_FIELDS = {"lat", "long", "loc_acc", "loc_type", "loc_bg", "loc_time_stamp", "ad_id"};
    private static final Set<String> LOCATION_FIELDS_SET = new HashSet(Arrays.asList(LOCATION_FIELDS));
    private static final Object syncLock = new Object() { // from class: com.onesignal.UserState.1
    };

    protected abstract void addDependFields();

    abstract boolean isSubscribed();

    abstract UserState newInstance(String str);

    UserState(String str, boolean z) {
        this.persistKey = str;
        if (z) {
            loadState();
        } else {
            this.dependValues = new JSONObject();
            this.syncValues = new JSONObject();
        }
    }

    UserState deepClone(String str) {
        UserState userStateNewInstance = newInstance(str);
        try {
            userStateNewInstance.dependValues = new JSONObject(this.dependValues.toString());
            userStateNewInstance.syncValues = new JSONObject(this.syncValues.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return userStateNewInstance;
    }

    private Set<String> getGroupChangeFields(UserState userState) {
        try {
            if (this.dependValues.optLong("loc_time_stamp") == userState.dependValues.getLong("loc_time_stamp")) {
                return null;
            }
            userState.syncValues.put("loc_bg", userState.dependValues.opt("loc_bg"));
            userState.syncValues.put("loc_time_stamp", userState.dependValues.opt("loc_time_stamp"));
            return LOCATION_FIELDS_SET;
        } catch (Throwable unused) {
            return null;
        }
    }

    void setLocation(LocationGMS.LocationPoint locationPoint) {
        try {
            this.syncValues.put("lat", locationPoint.lat);
            this.syncValues.put("long", locationPoint.log);
            this.syncValues.put("loc_acc", locationPoint.accuracy);
            this.syncValues.put("loc_type", locationPoint.type);
            this.dependValues.put("loc_bg", locationPoint.bg);
            this.dependValues.put("loc_time_stamp", locationPoint.timeStamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void clearLocation() {
        try {
            this.syncValues.put("lat", (Object) null);
            this.syncValues.put("long", (Object) null);
            this.syncValues.put("loc_acc", (Object) null);
            this.syncValues.put("loc_type", (Object) null);
            this.syncValues.put("loc_bg", (Object) null);
            this.syncValues.put("loc_time_stamp", (Object) null);
            this.dependValues.put("loc_bg", (Object) null);
            this.dependValues.put("loc_time_stamp", (Object) null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    JSONObject generateJsonDiff(UserState userState, boolean z) {
        addDependFields();
        userState.addDependFields();
        JSONObject jSONObjectGenerateJsonDiff = generateJsonDiff(this.syncValues, userState.syncValues, null, getGroupChangeFields(userState));
        if (!z && jSONObjectGenerateJsonDiff.toString().equals("{}")) {
            return null;
        }
        try {
            if (!jSONObjectGenerateJsonDiff.has("app_id")) {
                jSONObjectGenerateJsonDiff.put("app_id", this.syncValues.optString("app_id"));
            }
            if (this.syncValues.has("email_auth_hash")) {
                jSONObjectGenerateJsonDiff.put("email_auth_hash", this.syncValues.optString("email_auth_hash"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jSONObjectGenerateJsonDiff;
    }

    void set(String str, Object obj) {
        try {
            this.syncValues.put(str, obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadState() {
        int i;
        String string = OneSignalPrefs.getString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_USERSTATE_DEPENDVALYES_ + this.persistKey, null);
        if (string == null) {
            this.dependValues = new JSONObject();
            try {
                boolean z = true;
                if (this.persistKey.equals("CURRENT_STATE")) {
                    i = OneSignalPrefs.getInt(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_SUBSCRIPTION, 1);
                } else {
                    i = OneSignalPrefs.getInt(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_SYNCED_SUBSCRIPTION, 1);
                }
                if (i == -2) {
                    z = false;
                    i = 1;
                }
                this.dependValues.put("subscribableStatus", i);
                this.dependValues.put("userSubscribePref", z);
            } catch (JSONException unused) {
            }
        } else {
            try {
                this.dependValues = new JSONObject(string);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String string2 = OneSignalPrefs.getString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_USERSTATE_SYNCVALYES_ + this.persistKey, null);
        try {
            if (string2 == null) {
                this.syncValues = new JSONObject();
                this.syncValues.put("identifier", OneSignalPrefs.getString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_REGISTRATION_ID, null));
            } else {
                this.syncValues = new JSONObject(string2);
            }
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
    }

    void persistState() {
        synchronized (syncLock) {
            modifySyncValuesJsonArray("pkgs");
            OneSignalPrefs.saveString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_USERSTATE_SYNCVALYES_ + this.persistKey, this.syncValues.toString());
            OneSignalPrefs.saveString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_USERSTATE_DEPENDVALYES_ + this.persistKey, this.dependValues.toString());
        }
    }

    private void modifySyncValuesJsonArray(String str) {
        try {
            JSONArray jSONArray = this.syncValues.has(str) ? this.syncValues.getJSONArray(str) : new JSONArray();
            JSONArray jSONArray2 = new JSONArray();
            if (this.syncValues.has(str + "_d")) {
                String stringNE = JSONUtils.toStringNE(this.syncValues.getJSONArray(str + "_d"));
                for (int i = 0; i < jSONArray.length(); i++) {
                    if (!stringNE.contains(jSONArray.getString(i))) {
                        jSONArray2.put(jSONArray.get(i));
                    }
                }
                jSONArray = jSONArray2;
            }
            if (this.syncValues.has(str + "_a")) {
                JSONArray jSONArray3 = this.syncValues.getJSONArray(str + "_a");
                for (int i2 = 0; i2 < jSONArray3.length(); i2++) {
                    jSONArray.put(jSONArray3.get(i2));
                }
            }
            this.syncValues.put(str, jSONArray);
            this.syncValues.remove(str + "_a");
            this.syncValues.remove(str + "_d");
        } catch (Throwable unused) {
        }
    }

    void persistStateAfterSync(JSONObject jSONObject, JSONObject jSONObject2) {
        if (jSONObject != null) {
            generateJsonDiff(this.dependValues, jSONObject, this.dependValues, null);
        }
        if (jSONObject2 != null) {
            generateJsonDiff(this.syncValues, jSONObject2, this.syncValues, null);
            mergeTags(jSONObject2, null);
        }
        if (jSONObject == null && jSONObject2 == null) {
            return;
        }
        persistState();
    }

    void mergeTags(JSONObject jSONObject, JSONObject jSONObject2) {
        JSONObject jSONObject3;
        synchronized (syncLock) {
            if (jSONObject.has("tags")) {
                if (this.syncValues.has("tags")) {
                    try {
                        jSONObject3 = new JSONObject(this.syncValues.optString("tags"));
                    } catch (JSONException unused) {
                        jSONObject3 = new JSONObject();
                    }
                } else {
                    jSONObject3 = new JSONObject();
                }
                JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("tags");
                Iterator<String> itKeys = jSONObjectOptJSONObject.keys();
                while (itKeys.hasNext()) {
                    try {
                        String next = itKeys.next();
                        if ("".equals(jSONObjectOptJSONObject.optString(next))) {
                            jSONObject3.remove(next);
                        } else if (jSONObject2 == null || !jSONObject2.has(next)) {
                            jSONObject3.put(next, jSONObjectOptJSONObject.optString(next));
                        }
                    } catch (Throwable unused2) {
                    }
                }
                if (jSONObject3.toString().equals("{}")) {
                    this.syncValues.remove("tags");
                } else {
                    this.syncValues.put("tags", jSONObject3);
                }
            }
        }
    }

    private static JSONObject generateJsonDiff(JSONObject jSONObject, JSONObject jSONObject2, JSONObject jSONObject3, Set<String> set) {
        JSONObject jSONObjectGenerateJsonDiff;
        synchronized (syncLock) {
            jSONObjectGenerateJsonDiff = JSONUtils.generateJsonDiff(jSONObject, jSONObject2, jSONObject3, set);
        }
        return jSONObjectGenerateJsonDiff;
    }
}
