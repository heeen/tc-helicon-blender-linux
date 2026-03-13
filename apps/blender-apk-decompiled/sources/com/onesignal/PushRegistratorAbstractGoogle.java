package com.onesignal;

import android.content.Context;
import com.onesignal.OneSignal;
import com.onesignal.PushRegistrator;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
abstract class PushRegistratorAbstractGoogle implements PushRegistrator {
    private static int REGISTRATION_RETRY_BACKOFF_MS = 10000;
    private static int REGISTRATION_RETRY_COUNT = 5;
    private boolean firedCallback;
    private Thread registerThread;
    private PushRegistrator.RegisteredHandler registeredHandler;

    abstract String getProviderName();

    abstract String getToken(String str) throws Throwable;

    PushRegistratorAbstractGoogle() {
    }

    @Override // com.onesignal.PushRegistrator
    public void registerForPush(Context context, String str, PushRegistrator.RegisteredHandler registeredHandler) {
        this.registeredHandler = registeredHandler;
        if (isValidProjectNumber(str, registeredHandler)) {
            internalRegisterForPush(str);
        }
    }

    private void internalRegisterForPush(String str) {
        try {
            if (GooglePlayServicesUpgradePrompt.isGMSInstalledAndEnabled()) {
                registerInBackground(str);
            } else {
                GooglePlayServicesUpgradePrompt.ShowUpdateGPSDialog();
                OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "'Google Play services' app not installed or disabled on the device.");
                this.registeredHandler.complete(null, -7);
            }
        } catch (Throwable th) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Could not register with " + getProviderName() + " due to an issue with your AndroidManifest.xml or with 'Google Play services'.", th);
            this.registeredHandler.complete(null, -8);
        }
    }

    private synchronized void registerInBackground(final String str) {
        if (this.registerThread == null || !this.registerThread.isAlive()) {
            this.registerThread = new Thread(new Runnable() { // from class: com.onesignal.PushRegistratorAbstractGoogle.1
                @Override // java.lang.Runnable
                public void run() {
                    int i = 0;
                    while (i < PushRegistratorAbstractGoogle.REGISTRATION_RETRY_COUNT && !PushRegistratorAbstractGoogle.this.attemptRegistration(str, i)) {
                        i++;
                        OSUtils.sleep(PushRegistratorAbstractGoogle.REGISTRATION_RETRY_BACKOFF_MS * i);
                    }
                }
            });
            this.registerThread.start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean attemptRegistration(String str, int i) {
        try {
            String token = getToken(str);
            OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "Device registered, push token = " + token);
            this.registeredHandler.complete(token, 1);
            return true;
        } catch (IOException e) {
            if (!"SERVICE_NOT_AVAILABLE".equals(e.getMessage())) {
                OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error Getting " + getProviderName() + " Token", e);
                if (!this.firedCallback) {
                    this.registeredHandler.complete(null, -11);
                }
                return true;
            }
            if (i >= REGISTRATION_RETRY_COUNT - 1) {
                OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Retry count of " + REGISTRATION_RETRY_COUNT + " exceed! Could not get a " + getProviderName() + " Token.", e);
                return false;
            }
            OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "'Google Play services' returned SERVICE_NOT_AVAILABLE error. Current retry count: " + i, e);
            if (i != 2) {
                return false;
            }
            this.registeredHandler.complete(null, -9);
            this.firedCallback = true;
            return true;
        } catch (Throwable th) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Unknown error getting " + getProviderName() + " Token", th);
            this.registeredHandler.complete(null, -12);
            return true;
        }
    }

    private boolean isValidProjectNumber(String str, PushRegistrator.RegisteredHandler registeredHandler) {
        boolean z;
        try {
            Float.parseFloat(str);
            z = true;
        } catch (Throwable unused) {
            z = false;
        }
        if (z) {
            return true;
        }
        OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Missing Google Project number!\nPlease enter a Google Project number / Sender ID on under App Settings > Android > Configuration on the OneSignal dashboard.");
        registeredHandler.complete(null, -6);
        return false;
    }
}
