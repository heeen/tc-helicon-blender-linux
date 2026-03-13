package com.onesignal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.amazon.device.messaging.ADMMessageReceiver;
import com.onesignal.OneSignal;

/* JADX INFO: loaded from: classes.dex */
public class ADMMessageHandler extends ADMMessageHandlerBase {

    public static class Receiver extends ADMMessageReceiver {
        public Receiver() {
            super(ADMMessageHandler.class);
        }
    }

    public ADMMessageHandler() {
        super("ADMMessageHandler");
    }

    protected void onMessage(Intent intent) throws Throwable {
        Context applicationContext = getApplicationContext();
        Bundle extras = intent.getExtras();
        if (NotificationBundleProcessor.processBundleFromReceiver(applicationContext, extras).processed()) {
            return;
        }
        NotificationGenerationJob notificationGenerationJob = new NotificationGenerationJob(applicationContext);
        notificationGenerationJob.jsonPayload = NotificationBundleProcessor.bundleAsJSONObject(extras);
        NotificationBundleProcessor.ProcessJobForDisplay(notificationGenerationJob);
    }

    protected void onRegistered(String str) {
        OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "ADM registration ID: " + str);
        PushRegistratorADM.fireCallback(str);
    }

    protected void onRegistrationError(String str) {
        OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "ADM:onRegistrationError: " + str);
        if ("INVALID_SENDER".equals(str)) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Please double check that you have a matching package name (NOTE: Case Sensitive), api_key.txt, and the apk was signed with the same Keystore and Alias.");
        }
        PushRegistratorADM.fireCallback(null);
    }

    protected void onUnregistered(String str) {
        OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "ADM:onUnregistered: " + str);
    }
}
