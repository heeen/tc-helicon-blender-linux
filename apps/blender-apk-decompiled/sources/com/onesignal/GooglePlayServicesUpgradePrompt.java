package com.onesignal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import com.google.android.gms.common.GoogleApiAvailability;

/* JADX INFO: loaded from: classes.dex */
class GooglePlayServicesUpgradePrompt {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    GooglePlayServicesUpgradePrompt() {
    }

    static boolean isGMSInstalledAndEnabled() {
        try {
            return OneSignal.appContext.getPackageManager().getPackageInfo("com.google.android.gms", 128).applicationInfo.enabled;
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }

    private static boolean isGooglePlayStoreInstalled() {
        try {
            PackageManager packageManager = OneSignal.appContext.getPackageManager();
            String str = (String) packageManager.getPackageInfo("com.google.android.gms", 128).applicationInfo.loadLabel(packageManager);
            if (str != null) {
                return !str.equals("Market");
            }
            return false;
        } catch (Throwable unused) {
            return false;
        }
    }

    static void ShowUpdateGPSDialog() {
        if (isGMSInstalledAndEnabled() || !isGooglePlayStoreInstalled() || OneSignalPrefs.getBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_DO_NOT_SHOW_MISSING_GPS, false)) {
            return;
        }
        OSUtils.runOnMainUIThread(new Runnable() { // from class: com.onesignal.GooglePlayServicesUpgradePrompt.1
            @Override // java.lang.Runnable
            public void run() {
                final Activity activity = ActivityLifecycleHandler.curActivity;
                if (activity == null || OneSignal.mInitBuilder.mDisableGmsMissingPrompt) {
                    return;
                }
                String resourceString = OSUtils.getResourceString(activity, "onesignal_gms_missing_alert_text", "To receive push notifications please press 'Update' to enable 'Google Play services'.");
                String resourceString2 = OSUtils.getResourceString(activity, "onesignal_gms_missing_alert_button_update", "Update");
                String resourceString3 = OSUtils.getResourceString(activity, "onesignal_gms_missing_alert_button_skip", "Skip");
                new AlertDialog.Builder(activity).setMessage(resourceString).setPositiveButton(resourceString2, new DialogInterface.OnClickListener() { // from class: com.onesignal.GooglePlayServicesUpgradePrompt.1.2
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i) {
                        GooglePlayServicesUpgradePrompt.OpenPlayStoreToApp(activity);
                    }
                }).setNegativeButton(resourceString3, new DialogInterface.OnClickListener() { // from class: com.onesignal.GooglePlayServicesUpgradePrompt.1.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i) {
                        OneSignalPrefs.saveBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_DO_NOT_SHOW_MISSING_GPS, true);
                    }
                }).setNeutralButton(OSUtils.getResourceString(activity, "onesignal_gms_missing_alert_button_close", "Close"), (DialogInterface.OnClickListener) null).create().show();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void OpenPlayStoreToApp(Activity activity) {
        try {
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            googleApiAvailability.getErrorResolutionPendingIntent(activity, googleApiAvailability.isGooglePlayServicesAvailable(OneSignal.appContext), PLAY_SERVICES_RESOLUTION_REQUEST).send();
        } catch (PendingIntent.CanceledException unused) {
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
