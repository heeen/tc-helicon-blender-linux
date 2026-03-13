package com.onesignal;

import android.content.ComponentName;
import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;

/* JADX INFO: loaded from: classes.dex */
class PushRegistratorFCM extends PushRegistratorAbstractGoogle {
    private static final String FCM_APP_NAME = "ONESIGNAL_SDK_FCM_APP_NAME";
    private FirebaseApp firebaseApp;

    @Override // com.onesignal.PushRegistratorAbstractGoogle
    String getProviderName() {
        return FirebaseMessaging.INSTANCE_ID_SCOPE;
    }

    PushRegistratorFCM() {
    }

    static void disableFirebaseInstanceIdService(Context context) {
        try {
            context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, (Class<?>) FirebaseInstanceIdService.class), OSUtils.getResourceString(context, "gcm_defaultSenderId", null) == null ? 2 : 1, 1);
        } catch (IllegalArgumentException | NoClassDefFoundError unused) {
        }
    }

    @Override // com.onesignal.PushRegistratorAbstractGoogle
    String getToken(String str) throws Throwable {
        initFirebaseApp(str);
        return FirebaseInstanceId.getInstance(this.firebaseApp).getToken(str, FirebaseMessaging.INSTANCE_ID_SCOPE);
    }

    private void initFirebaseApp(String str) {
        if (this.firebaseApp != null) {
            return;
        }
        this.firebaseApp = FirebaseApp.initializeApp(OneSignal.appContext, new FirebaseOptions.Builder().setGcmSenderId(str).setApplicationId("OMIT_ID").setApiKey("OMIT_KEY").build(), FCM_APP_NAME);
    }
}
