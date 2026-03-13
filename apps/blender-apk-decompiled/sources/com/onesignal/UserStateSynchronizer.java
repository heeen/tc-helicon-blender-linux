package com.onesignal;

import android.os.Handler;
import android.os.HandlerThread;
import com.onesignal.LocationGMS;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalRestClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
abstract class UserStateSynchronizer {
    private boolean canMakeUpdates;
    protected UserState currentUserState;
    protected UserState toSyncUserState;
    protected final Object syncLock = new Object() { // from class: com.onesignal.UserStateSynchronizer.1
    };
    private AtomicBoolean runningSyncUserState = new AtomicBoolean();
    private ArrayList<OneSignal.ChangeTagsUpdateHandler> sendTagsHandlers = new ArrayList<>();
    HashMap<Integer, NetworkHandlerThread> networkHandlerThreads = new HashMap<>();
    private final Object networkHandlerSyncLock = new Object() { // from class: com.onesignal.UserStateSynchronizer.2
    };
    protected boolean waitingForSessionResponse = false;

    protected abstract void addOnSessionOrCreateExtras(JSONObject jSONObject);

    protected abstract void fireEventsForUpdateFailure(JSONObject jSONObject);

    protected abstract String getId();

    abstract boolean getSubscribed();

    abstract GetTagsResult getTags(boolean z);

    public abstract boolean getUserSubscribePreference();

    abstract void logoutEmail();

    protected abstract UserState newUserState(String str, boolean z);

    protected abstract void onSuccessfulSync(JSONObject jSONObject);

    protected abstract void scheduleSyncToServer();

    public abstract void setPermission(boolean z);

    abstract void setSubscription(boolean z);

    abstract void updateIdDependents(String str);

    abstract void updateState(JSONObject jSONObject);

    UserStateSynchronizer() {
    }

    static class GetTagsResult {
        JSONObject result;
        boolean serverSuccess;

        GetTagsResult(boolean z, JSONObject jSONObject) {
            this.serverSuccess = z;
            this.result = jSONObject;
        }
    }

    String getRegistrationId() {
        return getToSyncUserState().syncValues.optString("identifier", null);
    }

    class NetworkHandlerThread extends HandlerThread {
        static final int MAX_RETRIES = 3;
        static final int NETWORK_CALL_DELAY_TO_BUFFER_MS = 5000;
        protected static final int NETWORK_HANDLER_USERSTATE = 0;
        int currentRetry;
        Handler mHandler;
        int mType;

        NetworkHandlerThread(int i) {
            super("OSH_NetworkHandlerThread");
            this.mHandler = null;
            this.mType = i;
            start();
            this.mHandler = new Handler(getLooper());
        }

        void runNewJobDelayed() {
            if (UserStateSynchronizer.this.canMakeUpdates) {
                synchronized (this.mHandler) {
                    this.currentRetry = 0;
                    this.mHandler.removeCallbacksAndMessages(null);
                    this.mHandler.postDelayed(getNewRunnable(), 5000L);
                }
            }
        }

        private Runnable getNewRunnable() {
            if (this.mType != 0) {
                return null;
            }
            return new Runnable() { // from class: com.onesignal.UserStateSynchronizer.NetworkHandlerThread.1
                @Override // java.lang.Runnable
                public void run() {
                    if (UserStateSynchronizer.this.runningSyncUserState.get()) {
                        return;
                    }
                    UserStateSynchronizer.this.syncUserState(false);
                }
            };
        }

        void stopScheduledRunnable() {
            this.mHandler.removeCallbacksAndMessages(null);
        }

        boolean doRetry() {
            boolean zHasMessages;
            synchronized (this.mHandler) {
                boolean z = this.currentRetry < 3;
                boolean zHasMessages2 = this.mHandler.hasMessages(0);
                if (z && !zHasMessages2) {
                    this.currentRetry++;
                    this.mHandler.postDelayed(getNewRunnable(), this.currentRetry * 15000);
                }
                zHasMessages = this.mHandler.hasMessages(0);
            }
            return zHasMessages;
        }
    }

    protected JSONObject generateJsonDiff(JSONObject jSONObject, JSONObject jSONObject2, JSONObject jSONObject3, Set<String> set) {
        JSONObject jSONObjectGenerateJsonDiff;
        synchronized (this.syncLock) {
            jSONObjectGenerateJsonDiff = JSONUtils.generateJsonDiff(jSONObject, jSONObject2, jSONObject3, set);
        }
        return jSONObjectGenerateJsonDiff;
    }

    protected UserState getToSyncUserState() {
        synchronized (this.syncLock) {
            if (this.toSyncUserState == null) {
                this.toSyncUserState = newUserState("TOSYNC_STATE", true);
            }
        }
        return this.toSyncUserState;
    }

    void initUserState() {
        synchronized (this.syncLock) {
            if (this.currentUserState == null) {
                this.currentUserState = newUserState("CURRENT_STATE", true);
            }
        }
        getToSyncUserState();
    }

    void clearLocation() {
        getToSyncUserState().clearLocation();
        getToSyncUserState().persistState();
    }

    boolean persist() {
        boolean z;
        if (this.toSyncUserState == null) {
            return false;
        }
        synchronized (this.syncLock) {
            z = this.currentUserState.generateJsonDiff(this.toSyncUserState, isSessionCall()) != null;
            this.toSyncUserState.persistState();
        }
        return z;
    }

    private boolean isSessionCall() {
        return (getToSyncUserState().dependValues.optBoolean("session") || getId() == null) && !this.waitingForSessionResponse;
    }

    private boolean syncEmailLogout() {
        return getToSyncUserState().dependValues.optBoolean("logoutEmail", false);
    }

    void syncUserState(boolean z) {
        this.runningSyncUserState.set(true);
        internalSyncUserState(z);
        this.runningSyncUserState.set(false);
    }

    private void internalSyncUserState(boolean z) {
        String id = getId();
        if (syncEmailLogout() && id != null) {
            doEmailLogout(id);
            return;
        }
        if (this.currentUserState == null) {
            initUserState();
        }
        boolean z2 = !z && isSessionCall();
        synchronized (this.syncLock) {
            JSONObject jSONObjectGenerateJsonDiff = this.currentUserState.generateJsonDiff(getToSyncUserState(), z2);
            JSONObject jSONObjectGenerateJsonDiff2 = generateJsonDiff(this.currentUserState.dependValues, getToSyncUserState().dependValues, null, null);
            if (jSONObjectGenerateJsonDiff == null) {
                this.currentUserState.persistStateAfterSync(jSONObjectGenerateJsonDiff2, null);
                for (OneSignal.ChangeTagsUpdateHandler changeTagsUpdateHandler : this.sendTagsHandlers) {
                    if (changeTagsUpdateHandler != null) {
                        changeTagsUpdateHandler.onSuccess(OneSignalStateSynchronizer.getTags(false).result);
                    }
                }
                this.sendTagsHandlers.clear();
                return;
            }
            getToSyncUserState().persistState();
            if (!z2) {
                doPutSync(id, jSONObjectGenerateJsonDiff, jSONObjectGenerateJsonDiff2);
            } else {
                doCreateOrNewSession(id, jSONObjectGenerateJsonDiff, jSONObjectGenerateJsonDiff2);
            }
        }
    }

    private void doEmailLogout(String str) {
        String str2 = "players/" + str + "/email_logout";
        JSONObject jSONObject = new JSONObject();
        try {
            JSONObject jSONObject2 = this.currentUserState.dependValues;
            if (jSONObject2.has("email_auth_hash")) {
                jSONObject.put("email_auth_hash", jSONObject2.optString("email_auth_hash"));
            }
            JSONObject jSONObject3 = this.currentUserState.syncValues;
            if (jSONObject3.has("parent_player_id")) {
                jSONObject.put("parent_player_id", jSONObject3.optString("parent_player_id"));
            }
            jSONObject.put("app_id", jSONObject3.optString("app_id"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        OneSignalRestClient.postSync(str2, jSONObject, new OneSignalRestClient.ResponseHandler() { // from class: com.onesignal.UserStateSynchronizer.3
            @Override // com.onesignal.OneSignalRestClient.ResponseHandler
            void onFailure(int i, String str3, Throwable th) {
                OneSignal.Log(OneSignal.LOG_LEVEL.WARN, "Failed last request. statusCode: " + i + "\nresponse: " + str3);
                if (UserStateSynchronizer.this.response400WithErrorsContaining(i, str3, "already logged out of email")) {
                    UserStateSynchronizer.this.logoutEmailSyncSuccess();
                } else if (UserStateSynchronizer.this.response400WithErrorsContaining(i, str3, "not a valid device_type")) {
                    UserStateSynchronizer.this.handlePlayerDeletedFromServer();
                } else {
                    UserStateSynchronizer.this.handleNetworkFailure(i);
                }
            }

            @Override // com.onesignal.OneSignalRestClient.ResponseHandler
            void onSuccess(String str3) {
                UserStateSynchronizer.this.logoutEmailSyncSuccess();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logoutEmailSyncSuccess() {
        getToSyncUserState().dependValues.remove("logoutEmail");
        this.toSyncUserState.dependValues.remove("email_auth_hash");
        this.toSyncUserState.syncValues.remove("parent_player_id");
        this.toSyncUserState.persistState();
        this.currentUserState.dependValues.remove("email_auth_hash");
        this.currentUserState.syncValues.remove("parent_player_id");
        String strOptString = this.currentUserState.syncValues.optString("email");
        this.currentUserState.syncValues.remove("email");
        OneSignalStateSynchronizer.setNewSessionForEmail();
        OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "Device successfully logged out of email: " + strOptString);
        OneSignal.handleSuccessfulEmailLogout();
    }

    private void doPutSync(String str, final JSONObject jSONObject, final JSONObject jSONObject2) {
        if (str == null) {
            for (OneSignal.ChangeTagsUpdateHandler changeTagsUpdateHandler : this.sendTagsHandlers) {
                if (changeTagsUpdateHandler != null) {
                    changeTagsUpdateHandler.onFailure(new OneSignal.SendTagsError(-1, "Unable to update tags: the current user is not registered with OneSignal"));
                }
            }
            this.sendTagsHandlers.clear();
            return;
        }
        final ArrayList arrayList = (ArrayList) this.sendTagsHandlers.clone();
        this.sendTagsHandlers.clear();
        OneSignalRestClient.putSync("players/" + str, jSONObject, new OneSignalRestClient.ResponseHandler() { // from class: com.onesignal.UserStateSynchronizer.4
            @Override // com.onesignal.OneSignalRestClient.ResponseHandler
            void onFailure(int i, String str2, Throwable th) {
                OneSignal.Log(OneSignal.LOG_LEVEL.WARN, "Failed last request. statusCode: " + i + "\nresponse: " + str2);
                synchronized (UserStateSynchronizer.this.syncLock) {
                    if (UserStateSynchronizer.this.response400WithErrorsContaining(i, str2, "No user with this id found")) {
                        UserStateSynchronizer.this.handlePlayerDeletedFromServer();
                    } else {
                        UserStateSynchronizer.this.handleNetworkFailure(i);
                    }
                }
                if (jSONObject.has("tags")) {
                    for (OneSignal.ChangeTagsUpdateHandler changeTagsUpdateHandler2 : arrayList) {
                        if (changeTagsUpdateHandler2 != null) {
                            changeTagsUpdateHandler2.onFailure(new OneSignal.SendTagsError(i, str2));
                        }
                    }
                }
            }

            @Override // com.onesignal.OneSignalRestClient.ResponseHandler
            void onSuccess(String str2) {
                synchronized (UserStateSynchronizer.this.syncLock) {
                    UserStateSynchronizer.this.currentUserState.persistStateAfterSync(jSONObject2, jSONObject);
                    UserStateSynchronizer.this.onSuccessfulSync(jSONObject);
                }
                JSONObject jSONObject3 = OneSignalStateSynchronizer.getTags(false).result;
                if (!jSONObject.has("tags") || jSONObject3 == null) {
                    return;
                }
                for (OneSignal.ChangeTagsUpdateHandler changeTagsUpdateHandler2 : arrayList) {
                    if (changeTagsUpdateHandler2 != null) {
                        changeTagsUpdateHandler2.onSuccess(jSONObject3);
                    }
                }
            }
        });
    }

    private void doCreateOrNewSession(final String str, final JSONObject jSONObject, final JSONObject jSONObject2) {
        String str2;
        if (str == null) {
            str2 = "players";
        } else {
            str2 = "players/" + str + "/on_session";
        }
        this.waitingForSessionResponse = true;
        addOnSessionOrCreateExtras(jSONObject);
        OneSignalRestClient.postSync(str2, jSONObject, new OneSignalRestClient.ResponseHandler() { // from class: com.onesignal.UserStateSynchronizer.5
            @Override // com.onesignal.OneSignalRestClient.ResponseHandler
            void onFailure(int i, String str3, Throwable th) {
                synchronized (UserStateSynchronizer.this.syncLock) {
                    UserStateSynchronizer.this.waitingForSessionResponse = false;
                    OneSignal.Log(OneSignal.LOG_LEVEL.WARN, "Failed last request. statusCode: " + i + "\nresponse: " + str3);
                    if (UserStateSynchronizer.this.response400WithErrorsContaining(i, str3, "not a valid device_type")) {
                        UserStateSynchronizer.this.handlePlayerDeletedFromServer();
                    } else {
                        UserStateSynchronizer.this.handleNetworkFailure(i);
                    }
                }
            }

            @Override // com.onesignal.OneSignalRestClient.ResponseHandler
            void onSuccess(String str3) {
                synchronized (UserStateSynchronizer.this.syncLock) {
                    UserStateSynchronizer.this.waitingForSessionResponse = false;
                    UserStateSynchronizer.this.currentUserState.persistStateAfterSync(jSONObject2, jSONObject);
                    try {
                        JSONObject jSONObject3 = new JSONObject(str3);
                        if (jSONObject3.has("id")) {
                            String strOptString = jSONObject3.optString("id");
                            UserStateSynchronizer.this.updateIdDependents(strOptString);
                            OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "Device registered, UserId = " + strOptString);
                        } else {
                            OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "session sent, UserId = " + str);
                        }
                        UserStateSynchronizer.this.getUserStateForModification().dependValues.put("session", false);
                        UserStateSynchronizer.this.getUserStateForModification().persistState();
                        UserStateSynchronizer.this.onSuccessfulSync(jSONObject);
                    } catch (Throwable th) {
                        OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "ERROR parsing on_session or create JSON Response.", th);
                    }
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNetworkFailure(int i) {
        if (i == 403) {
            OneSignal.Log(OneSignal.LOG_LEVEL.FATAL, "403 error updating player, omitting further retries!");
            fireNetworkFailureEvents();
        } else {
            if (getNetworkHandlerThread(0).doRetry()) {
                return;
            }
            fireNetworkFailureEvents();
        }
    }

    private void fireNetworkFailureEvents() {
        JSONObject jSONObjectGenerateJsonDiff = this.currentUserState.generateJsonDiff(this.toSyncUserState, false);
        if (jSONObjectGenerateJsonDiff != null) {
            fireEventsForUpdateFailure(jSONObjectGenerateJsonDiff);
        }
        if (getToSyncUserState().dependValues.optBoolean("logoutEmail", false)) {
            OneSignal.handleFailedEmailLogout();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean response400WithErrorsContaining(int i, String str, String str2) {
        if (i == 400 && str != null) {
            try {
                JSONObject jSONObject = new JSONObject(str);
                if (jSONObject.has("errors")) {
                    return jSONObject.optString("errors").contains(str2);
                }
                return false;
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        return false;
    }

    protected NetworkHandlerThread getNetworkHandlerThread(Integer num) {
        NetworkHandlerThread networkHandlerThread;
        synchronized (this.networkHandlerSyncLock) {
            if (!this.networkHandlerThreads.containsKey(num)) {
                this.networkHandlerThreads.put(num, new NetworkHandlerThread(num.intValue()));
            }
            networkHandlerThread = this.networkHandlerThreads.get(num);
        }
        return networkHandlerThread;
    }

    protected UserState getUserStateForModification() {
        if (this.toSyncUserState == null) {
            this.toSyncUserState = this.currentUserState.deepClone("TOSYNC_STATE");
        }
        scheduleSyncToServer();
        return this.toSyncUserState;
    }

    void updateDeviceInfo(JSONObject jSONObject) {
        JSONObject jSONObject2 = getUserStateForModification().syncValues;
        generateJsonDiff(jSONObject2, jSONObject, jSONObject2, null);
    }

    void setNewSession() {
        try {
            synchronized (this.syncLock) {
                getUserStateForModification().dependValues.put("session", true);
                getUserStateForModification().persistState();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    boolean getSyncAsNewSession() {
        return getUserStateForModification().dependValues.optBoolean("session");
    }

    void sendTags(JSONObject jSONObject, OneSignal.ChangeTagsUpdateHandler changeTagsUpdateHandler) {
        this.sendTagsHandlers.add(changeTagsUpdateHandler);
        JSONObject jSONObject2 = getUserStateForModification().syncValues;
        generateJsonDiff(jSONObject2, jSONObject, jSONObject2, null);
    }

    void syncHashedEmail(JSONObject jSONObject) {
        JSONObject jSONObject2 = getUserStateForModification().syncValues;
        generateJsonDiff(jSONObject2, jSONObject, jSONObject2, null);
    }

    void setExternalUserId(String str) throws JSONException {
        getUserStateForModification().syncValues.put("external_user_id", str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePlayerDeletedFromServer() {
        OneSignal.handleSuccessfulEmailLogout();
        resetCurrentState();
        scheduleSyncToServer();
    }

    void resetCurrentState() {
        this.currentUserState.syncValues = new JSONObject();
        this.currentUserState.persistState();
    }

    void updateLocation(LocationGMS.LocationPoint locationPoint) {
        getUserStateForModification().setLocation(locationPoint);
    }

    void readyToUpdate(boolean z) {
        boolean z2 = this.canMakeUpdates != z;
        this.canMakeUpdates = z;
        if (z2 && z) {
            scheduleSyncToServer();
        }
    }
}
