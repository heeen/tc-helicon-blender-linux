package com.onesignal;

import com.onesignal.UserStateSynchronizer;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class UserStateEmailSynchronizer extends UserStateSynchronizer {
    @Override // com.onesignal.UserStateSynchronizer
    boolean getSubscribed() {
        return false;
    }

    @Override // com.onesignal.UserStateSynchronizer
    UserStateSynchronizer.GetTagsResult getTags(boolean z) {
        return null;
    }

    @Override // com.onesignal.UserStateSynchronizer
    public boolean getUserSubscribePreference() {
        return false;
    }

    @Override // com.onesignal.UserStateSynchronizer
    public void setPermission(boolean z) {
    }

    @Override // com.onesignal.UserStateSynchronizer
    void setSubscription(boolean z) {
    }

    @Override // com.onesignal.UserStateSynchronizer
    void updateState(JSONObject jSONObject) {
    }

    UserStateEmailSynchronizer() {
    }

    @Override // com.onesignal.UserStateSynchronizer
    protected UserState newUserState(String str, boolean z) {
        return new UserStateEmail(str, z);
    }

    void refresh() {
        scheduleSyncToServer();
    }

    @Override // com.onesignal.UserStateSynchronizer
    protected void scheduleSyncToServer() {
        if ((getId() == null && getRegistrationId() == null) || OneSignal.getUserId() == null) {
            return;
        }
        getNetworkHandlerThread(0).runNewJobDelayed();
    }

    /* JADX WARN: Removed duplicated region for block: B:11:0x0026  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    void setEmail(java.lang.String r6, java.lang.String r7) {
        /*
            r5 = this;
            com.onesignal.UserState r0 = r5.getUserStateForModification()
            org.json.JSONObject r0 = r0.syncValues
            java.lang.String r1 = "identifier"
            java.lang.String r1 = r0.optString(r1)
            boolean r1 = r6.equals(r1)
            if (r1 == 0) goto L26
            java.lang.String r1 = "email_auth_hash"
            java.lang.String r1 = r0.optString(r1)
            if (r7 != 0) goto L1d
            java.lang.String r2 = ""
            goto L1e
        L1d:
            r2 = r7
        L1e:
            boolean r1 = r1.equals(r2)
            if (r1 == 0) goto L26
            r1 = 1
            goto L27
        L26:
            r1 = 0
        L27:
            if (r1 == 0) goto L2d
            com.onesignal.OneSignal.fireEmailUpdateSuccess()
            return
        L2d:
            java.lang.String r1 = "identifier"
            r2 = 0
            java.lang.String r1 = r0.optString(r1, r2)
            if (r1 != 0) goto L39
            r5.setNewSession()
        L39:
            org.json.JSONObject r3 = new org.json.JSONObject     // Catch: org.json.JSONException -> L66
            r3.<init>()     // Catch: org.json.JSONException -> L66
            java.lang.String r4 = "identifier"
            r3.put(r4, r6)     // Catch: org.json.JSONException -> L66
            if (r7 == 0) goto L4a
            java.lang.String r4 = "email_auth_hash"
            r3.put(r4, r7)     // Catch: org.json.JSONException -> L66
        L4a:
            if (r7 != 0) goto L5f
            if (r1 == 0) goto L5f
            boolean r6 = r1.equals(r6)     // Catch: org.json.JSONException -> L66
            if (r6 != 0) goto L5f
            java.lang.String r6 = ""
            com.onesignal.OneSignal.saveEmailId(r6)     // Catch: org.json.JSONException -> L66
            r5.resetCurrentState()     // Catch: org.json.JSONException -> L66
            r5.setNewSession()     // Catch: org.json.JSONException -> L66
        L5f:
            r5.generateJsonDiff(r0, r3, r0, r2)     // Catch: org.json.JSONException -> L66
            r5.scheduleSyncToServer()     // Catch: org.json.JSONException -> L66
            goto L6a
        L66:
            r5 = move-exception
            r5.printStackTrace()
        L6a:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.onesignal.UserStateEmailSynchronizer.setEmail(java.lang.String, java.lang.String):void");
    }

    @Override // com.onesignal.UserStateSynchronizer
    protected String getId() {
        return OneSignal.getEmailId();
    }

    @Override // com.onesignal.UserStateSynchronizer
    void updateIdDependents(String str) {
        OneSignal.updateEmailIdDependents(str);
    }

    @Override // com.onesignal.UserStateSynchronizer
    protected void addOnSessionOrCreateExtras(JSONObject jSONObject) {
        try {
            jSONObject.put("device_type", 11);
            jSONObject.putOpt("device_player_id", OneSignal.getUserId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override // com.onesignal.UserStateSynchronizer
    void logoutEmail() {
        OneSignal.saveEmailId("");
        resetCurrentState();
        getToSyncUserState().syncValues.remove("identifier");
        this.toSyncUserState.syncValues.remove("email_auth_hash");
        this.toSyncUserState.syncValues.remove("device_player_id");
        this.toSyncUserState.persistState();
        OneSignal.getPermissionSubscriptionState().emailSubscriptionStatus.clearEmailAndId();
    }

    @Override // com.onesignal.UserStateSynchronizer
    protected void fireEventsForUpdateFailure(JSONObject jSONObject) {
        if (jSONObject.has("identifier")) {
            OneSignal.fireEmailUpdateFailure();
        }
    }

    @Override // com.onesignal.UserStateSynchronizer
    protected void onSuccessfulSync(JSONObject jSONObject) {
        if (jSONObject.has("identifier")) {
            OneSignal.fireEmailUpdateSuccess();
        }
    }
}
