package com.onesignal;

import android.support.annotation.NonNull;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalRestClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class OneSignalRemoteParams {
    private static final int INCREASE_BETWEEN_RETRIES = 10000;
    private static final int MAX_WAIT_BETWEEN_RETRIES = 90000;
    private static final int MIN_WAIT_BETWEEN_RETRIES = 30000;
    private static int androidParamsReties;

    interface CallBack {
        void complete(Params params);
    }

    OneSignalRemoteParams() {
    }

    static /* synthetic */ int access$008() {
        int i = androidParamsReties;
        androidParamsReties = i + 1;
        return i;
    }

    static class Params {
        JSONObject awl;
        boolean enterprise;
        boolean firebaseAnalytics;
        String googleProjectNumber;
        JSONArray notificationChannels;
        boolean restoreTTLFilter;
        boolean useEmailAuth;

        Params() {
        }
    }

    static void makeAndroidParamsRequest(@NonNull final CallBack callBack) {
        OneSignalRestClient.ResponseHandler responseHandler = new OneSignalRestClient.ResponseHandler() { // from class: com.onesignal.OneSignalRemoteParams.1
            @Override // com.onesignal.OneSignalRestClient.ResponseHandler
            void onFailure(int i, String str, Throwable th) {
                if (i == 403) {
                    OneSignal.Log(OneSignal.LOG_LEVEL.FATAL, "403 error getting OneSignal params, omitting further retries!");
                } else {
                    new Thread(new Runnable() { // from class: com.onesignal.OneSignalRemoteParams.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            int i2 = (OneSignalRemoteParams.androidParamsReties * OneSignalRemoteParams.INCREASE_BETWEEN_RETRIES) + OneSignalRemoteParams.MIN_WAIT_BETWEEN_RETRIES;
                            if (i2 > OneSignalRemoteParams.MAX_WAIT_BETWEEN_RETRIES) {
                                i2 = OneSignalRemoteParams.MAX_WAIT_BETWEEN_RETRIES;
                            }
                            OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "Failed to get Android parameters, trying again in " + (i2 / 1000) + " seconds.");
                            OSUtils.sleep(i2);
                            OneSignalRemoteParams.access$008();
                            OneSignalRemoteParams.makeAndroidParamsRequest(callBack);
                        }
                    }, "OS_PARAMS_REQUEST").start();
                }
            }

            @Override // com.onesignal.OneSignalRestClient.ResponseHandler
            void onSuccess(String str) {
                OneSignalRemoteParams.processJson(str, callBack);
            }
        };
        String str = "apps/" + OneSignal.appId + "/android_params.js";
        String userId = OneSignal.getUserId();
        if (userId != null) {
            str = str + "?player_id=" + userId;
        }
        OneSignal.Log(OneSignal.LOG_LEVEL.DEBUG, "Starting request to get Android parameters.");
        OneSignalRestClient.get(str, responseHandler, "CACHE_KEY_REMOTE_PARAMS");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void processJson(String str, @NonNull CallBack callBack) {
        try {
            final JSONObject jSONObject = new JSONObject(str);
            callBack.complete(new Params() { // from class: com.onesignal.OneSignalRemoteParams.2
                {
                    this.enterprise = jSONObject.optBoolean("enterp", false);
                    this.useEmailAuth = jSONObject.optBoolean("use_email_auth", false);
                    this.awl = jSONObject.optJSONObject("awl_list");
                    this.notificationChannels = jSONObject.optJSONArray("chnl_lst");
                    this.firebaseAnalytics = jSONObject.optBoolean("fba", false);
                    this.restoreTTLFilter = jSONObject.optBoolean("restore_ttl_filter", true);
                    this.googleProjectNumber = jSONObject.optString("android_sender_id", null);
                }
            });
        } catch (NullPointerException | JSONException e) {
            OneSignal.Log(OneSignal.LOG_LEVEL.FATAL, "Error parsing android_params!: ", e);
            OneSignal.Log(OneSignal.LOG_LEVEL.FATAL, "Response that errored from android_params!: " + str);
        }
    }
}
