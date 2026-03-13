package com.onesignal;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v7.widget.ActivityChooserView;
import java.security.SecureRandom;

/* JADX INFO: loaded from: classes.dex */
class OneSignalChromeTab {
    private static boolean opened;

    OneSignalChromeTab() {
    }

    static void setup(Context context, String str, String str2, String str3) {
        if (opened || OneSignal.remoteParams.enterprise || str2 == null) {
            return;
        }
        try {
            Class.forName("android.support.customtabs.CustomTabsServiceConnection");
            String str4 = "?app_id=" + str + "&user_id=" + str2;
            if (str3 != null) {
                str4 = str4 + "&ad_id=" + str3;
            }
            opened = CustomTabsClient.bindCustomTabsService(context, "com.android.chrome", new OneSignalCustomTabsServiceConnection(context, str4 + "&cbs_id=" + new SecureRandom().nextInt(ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED)));
        } catch (ClassNotFoundException unused) {
        }
    }

    private static class OneSignalCustomTabsServiceConnection extends CustomTabsServiceConnection {
        private Context mContext;
        private String mParams;

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
        }

        OneSignalCustomTabsServiceConnection(Context context, String str) {
            this.mContext = context;
            this.mParams = str;
        }

        @Override // android.support.customtabs.CustomTabsServiceConnection
        public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
            if (customTabsClient == null) {
                return;
            }
            customTabsClient.warmup(0L);
            CustomTabsSession customTabsSessionNewSession = customTabsClient.newSession(new CustomTabsCallback() { // from class: com.onesignal.OneSignalChromeTab.OneSignalCustomTabsServiceConnection.1
                @Override // android.support.customtabs.CustomTabsCallback
                public void onNavigationEvent(int i, Bundle bundle) {
                    super.onNavigationEvent(i, bundle);
                }

                @Override // android.support.customtabs.CustomTabsCallback
                public void extraCallback(String str, Bundle bundle) {
                    super.extraCallback(str, bundle);
                }
            });
            if (customTabsSessionNewSession == null) {
                return;
            }
            customTabsSessionNewSession.mayLaunchUrl(Uri.parse("https://onesignal.com/android_frame.html" + this.mParams), null, null);
        }
    }
}
