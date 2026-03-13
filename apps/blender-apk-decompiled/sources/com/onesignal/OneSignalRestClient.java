package com.onesignal;

import android.support.annotation.NonNull;
import java.io.IOException;
import java.lang.Thread;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class OneSignalRestClient {
    private static final String BASE_URL = "https://onesignal.com/api/v1/";
    static final String CACHE_KEY_GET_TAGS = "CACHE_KEY_GET_TAGS";
    static final String CACHE_KEY_REMOTE_PARAMS = "CACHE_KEY_REMOTE_PARAMS";
    private static final int GET_TIMEOUT = 60000;
    private static final int TIMEOUT = 120000;

    private static int getThreadTimeout(int i) {
        return i + 5000;
    }

    static abstract class ResponseHandler {
        void onFailure(int i, String str, Throwable th) {
        }

        void onSuccess(String str) {
        }

        ResponseHandler() {
        }
    }

    OneSignalRestClient() {
    }

    public static void put(final String str, final JSONObject jSONObject, final ResponseHandler responseHandler) {
        new Thread(new Runnable() { // from class: com.onesignal.OneSignalRestClient.1
            @Override // java.lang.Runnable
            public void run() {
                OneSignalRestClient.makeRequest(str, "PUT", jSONObject, responseHandler, OneSignalRestClient.TIMEOUT, null);
            }
        }).start();
    }

    public static void post(final String str, final JSONObject jSONObject, final ResponseHandler responseHandler) {
        new Thread(new Runnable() { // from class: com.onesignal.OneSignalRestClient.2
            @Override // java.lang.Runnable
            public void run() {
                OneSignalRestClient.makeRequest(str, "POST", jSONObject, responseHandler, OneSignalRestClient.TIMEOUT, null);
            }
        }).start();
    }

    public static void get(final String str, final ResponseHandler responseHandler, @NonNull final String str2) {
        new Thread(new Runnable() { // from class: com.onesignal.OneSignalRestClient.3
            @Override // java.lang.Runnable
            public void run() {
                OneSignalRestClient.makeRequest(str, null, null, responseHandler, OneSignalRestClient.GET_TIMEOUT, str2);
            }
        }).start();
    }

    public static void getSync(String str, ResponseHandler responseHandler, @NonNull String str2) {
        makeRequest(str, null, null, responseHandler, GET_TIMEOUT, str2);
    }

    public static void putSync(String str, JSONObject jSONObject, ResponseHandler responseHandler) {
        makeRequest(str, "PUT", jSONObject, responseHandler, TIMEOUT, null);
    }

    public static void postSync(String str, JSONObject jSONObject, ResponseHandler responseHandler) {
        makeRequest(str, "POST", jSONObject, responseHandler, TIMEOUT, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void makeRequest(final String str, final String str2, final JSONObject jSONObject, final ResponseHandler responseHandler, final int i, final String str3) {
        if (str2 == null || !OneSignal.shouldLogUserPrivacyConsentErrorMessageForMethodName(null)) {
            final Thread[] threadArr = new Thread[1];
            Thread thread = new Thread(new Runnable() { // from class: com.onesignal.OneSignalRestClient.4
                @Override // java.lang.Runnable
                public void run() {
                    threadArr[0] = OneSignalRestClient.startHTTPConnection(str, str2, jSONObject, responseHandler, i, str3);
                }
            }, "OS_HTTPConnection");
            thread.start();
            try {
                thread.join(getThreadTimeout(i));
                if (thread.getState() != Thread.State.TERMINATED) {
                    thread.interrupt();
                }
                if (threadArr[0] != null) {
                    threadArr[0].join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:52:0x0249 A[PHI: r2 r5
      0x0249: PHI (r2v5 java.net.HttpURLConnection) = (r2v4 java.net.HttpURLConnection), (r2v7 java.net.HttpURLConnection) binds: [B:70:0x029e, B:51:0x0247] A[DONT_GENERATE, DONT_INLINE]
      0x0249: PHI (r5v8 java.lang.Thread) = (r5v6 java.lang.Thread), (r5v21 java.lang.Thread) binds: [B:70:0x029e, B:51:0x0247] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Removed duplicated region for block: B:68:0x027c A[Catch: all -> 0x02a2, TryCatch #2 {all -> 0x02a2, blocks: (B:5:0x001d, B:7:0x0030, B:9:0x0035, B:11:0x0044, B:13:0x0079, B:15:0x0092, B:16:0x00ad, B:17:0x00b1, B:21:0x00cf, B:23:0x00eb, B:25:0x00f1, B:27:0x0104, B:29:0x010b, B:31:0x0150, B:30:0x012d, B:32:0x0156, B:36:0x017f, B:37:0x0197, B:39:0x01c4, B:41:0x01cb, B:45:0x01e0, B:47:0x01f4, B:49:0x01fc, B:50:0x0243, B:62:0x0257, B:64:0x025b, B:67:0x0260, B:69:0x029a, B:68:0x027c), top: B:78:0x001d }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static java.lang.Thread startHTTPConnection(java.lang.String r5, java.lang.String r6, org.json.JSONObject r7, com.onesignal.OneSignalRestClient.ResponseHandler r8, int r9, @android.support.annotation.Nullable java.lang.String r10) throws java.lang.Throwable {
        /*
            Method dump skipped, instruction units count: 681
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.onesignal.OneSignalRestClient.startHTTPConnection(java.lang.String, java.lang.String, org.json.JSONObject, com.onesignal.OneSignalRestClient$ResponseHandler, int, java.lang.String):java.lang.Thread");
    }

    private static Thread callResponseHandlerOnSuccess(final ResponseHandler responseHandler, final String str) {
        if (responseHandler == null) {
            return null;
        }
        Thread thread = new Thread(new Runnable() { // from class: com.onesignal.OneSignalRestClient.5
            @Override // java.lang.Runnable
            public void run() {
                responseHandler.onSuccess(str);
            }
        });
        thread.start();
        return thread;
    }

    private static Thread callResponseHandlerOnFailure(final ResponseHandler responseHandler, final int i, final String str, final Throwable th) {
        if (responseHandler == null) {
            return null;
        }
        Thread thread = new Thread(new Runnable() { // from class: com.onesignal.OneSignalRestClient.6
            @Override // java.lang.Runnable
            public void run() {
                responseHandler.onFailure(i, str, th);
            }
        });
        thread.start();
        return thread;
    }

    private static HttpURLConnection newHttpURLConnection(String str) throws IOException {
        return (HttpURLConnection) new URL(BASE_URL + str).openConnection();
    }
}
