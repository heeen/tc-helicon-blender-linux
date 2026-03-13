package com.onesignal;

import com.onesignal.OneSignalRestClient;
import com.onesignal.UserStateSynchronizer;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class UserStatePushSynchronizer extends UserStateSynchronizer {
    private static boolean serverSuccess;

    @Override // com.onesignal.UserStateSynchronizer
    protected void addOnSessionOrCreateExtras(JSONObject jSONObject) {
    }

    UserStatePushSynchronizer() {
    }

    @Override // com.onesignal.UserStateSynchronizer
    protected UserState newUserState(String str, boolean z) {
        return new UserStatePush(str, z);
    }

    @Override // com.onesignal.UserStateSynchronizer
    boolean getSubscribed() {
        return getToSyncUserState().isSubscribed();
    }

    @Override // com.onesignal.UserStateSynchronizer
    UserStateSynchronizer.GetTagsResult getTags(boolean z) {
        UserStateSynchronizer.GetTagsResult getTagsResult;
        if (z) {
            OneSignalRestClient.getSync("players/" + OneSignal.getUserId() + "?app_id=" + OneSignal.getSavedAppId(), new OneSignalRestClient.ResponseHandler() { // from class: com.onesignal.UserStatePushSynchronizer.1
                @Override // com.onesignal.OneSignalRestClient.ResponseHandler
                void onSuccess(String str) {
                    boolean unused = UserStatePushSynchronizer.serverSuccess = true;
                    try {
                        JSONObject jSONObject = new JSONObject(str);
                        if (jSONObject.has("tags")) {
                            synchronized (UserStatePushSynchronizer.this.syncLock) {
                                JSONObject jSONObjectGenerateJsonDiff = UserStatePushSynchronizer.this.generateJsonDiff(UserStatePushSynchronizer.this.currentUserState.syncValues.optJSONObject("tags"), UserStatePushSynchronizer.this.getToSyncUserState().syncValues.optJSONObject("tags"), null, null);
                                UserStatePushSynchronizer.this.currentUserState.syncValues.put("tags", jSONObject.optJSONObject("tags"));
                                UserStatePushSynchronizer.this.currentUserState.persistState();
                                UserStatePushSynchronizer.this.getToSyncUserState().mergeTags(jSONObject, jSONObjectGenerateJsonDiff);
                                UserStatePushSynchronizer.this.getToSyncUserState().persistState();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, "CACHE_KEY_GET_TAGS");
        }
        synchronized (this.syncLock) {
            getTagsResult = new UserStateSynchronizer.GetTagsResult(serverSuccess, JSONUtils.getJSONObjectWithoutBlankValues(this.toSyncUserState.syncValues, "tags"));
        }
        return getTagsResult;
    }

    @Override // com.onesignal.UserStateSynchronizer
    protected void scheduleSyncToServer() {
        getNetworkHandlerThread(0).runNewJobDelayed();
    }

    @Override // com.onesignal.UserStateSynchronizer
    void updateState(JSONObject jSONObject) {
        try {
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.putOpt("identifier", jSONObject.optString("identifier", null));
            if (jSONObject.has("device_type")) {
                jSONObject2.put("device_type", jSONObject.optInt("device_type"));
            }
            jSONObject2.putOpt("parent_player_id", jSONObject.optString("parent_player_id", null));
            JSONObject jSONObject3 = getUserStateForModification().syncValues;
            generateJsonDiff(jSONObject3, jSONObject2, jSONObject3, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            JSONObject jSONObject4 = new JSONObject();
            if (jSONObject.has("subscribableStatus")) {
                jSONObject4.put("subscribableStatus", jSONObject.optInt("subscribableStatus"));
            }
            if (jSONObject.has("androidPermission")) {
                jSONObject4.put("androidPermission", jSONObject.optBoolean("androidPermission"));
            }
            JSONObject jSONObject5 = getUserStateForModification().dependValues;
            generateJsonDiff(jSONObject5, jSONObject4, jSONObject5, null);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
    }

    void setEmail(String str, String str2) {
        try {
            UserState userStateForModification = getUserStateForModification();
            userStateForModification.dependValues.put("email_auth_hash", str2);
            JSONObject jSONObject = userStateForModification.syncValues;
            generateJsonDiff(jSONObject, new JSONObject().put("email", str), jSONObject, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override // com.onesignal.UserStateSynchronizer
    void setSubscription(boolean z) {
        try {
            getUserStateForModification().dependValues.put("userSubscribePref", z);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override // com.onesignal.UserStateSynchronizer
    public boolean getUserSubscribePreference() {
        return getToSyncUserState().dependValues.optBoolean("userSubscribePref", true);
    }

    @Override // com.onesignal.UserStateSynchronizer
    public void setPermission(boolean z) {
        try {
            getUserStateForModification().dependValues.put("androidPermission", z);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override // com.onesignal.UserStateSynchronizer
    protected String getId() {
        return OneSignal.getUserId();
    }

    @Override // com.onesignal.UserStateSynchronizer
    void updateIdDependents(String str) {
        OneSignal.updateUserIdDependents(str);
    }

    @Override // com.onesignal.UserStateSynchronizer
    void logoutEmail() {
        try {
            getUserStateForModification().dependValues.put("logoutEmail", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override // com.onesignal.UserStateSynchronizer
    protected void fireEventsForUpdateFailure(JSONObject jSONObject) {
        if (jSONObject.has("email")) {
            OneSignal.fireEmailUpdateFailure();
        }
    }

    @Override // com.onesignal.UserStateSynchronizer
    protected void onSuccessfulSync(JSONObject jSONObject) {
        if (jSONObject.has("email")) {
            OneSignal.fireEmailUpdateSuccess();
        }
        if (jSONObject.has("identifier")) {
            OneSignal.fireIdsAvailableCallback();
        }
    }
}
