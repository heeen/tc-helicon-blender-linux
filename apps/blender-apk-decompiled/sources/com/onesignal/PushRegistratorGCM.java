package com.onesignal;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/* JADX INFO: loaded from: classes.dex */
class PushRegistratorGCM extends PushRegistratorAbstractGoogle {
    @Override // com.onesignal.PushRegistratorAbstractGoogle
    String getProviderName() {
        return "GCM";
    }

    PushRegistratorGCM() {
    }

    @Override // com.onesignal.PushRegistratorAbstractGoogle
    String getToken(String str) throws Throwable {
        return GoogleCloudMessaging.getInstance(OneSignal.appContext).register(new String[]{str});
    }
}
