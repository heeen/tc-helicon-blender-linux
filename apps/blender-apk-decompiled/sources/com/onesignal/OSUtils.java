package com.onesignal;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.TelephonyManager;
import com.flurry.android.Constants;
import com.onesignal.OneSignal;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class OSUtils {
    static final int UNINITIALIZABLE_STATUS = -999;

    OSUtils() {
    }

    int initializationChecker(Context context, int i, String str) {
        Integer numCheckForGooglePushLibrary;
        try {
            UUID.fromString(str);
            if ("b2f7f966-d8cc-11e4-bed1-df8f05be55ba".equals(str) || "5eb5a37e-b458-11e3-ac11-000c2940e62c".equals(str)) {
                OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "OneSignal Example AppID detected, please update to your app's id found on OneSignal.com");
            }
            int iIntValue = 1;
            if (i == 1 && (numCheckForGooglePushLibrary = checkForGooglePushLibrary()) != null) {
                iIntValue = numCheckForGooglePushLibrary.intValue();
            }
            Integer numCheckAndroidSupportLibrary = checkAndroidSupportLibrary(context);
            return numCheckAndroidSupportLibrary != null ? numCheckAndroidSupportLibrary.intValue() : iIntValue;
        } catch (Throwable th) {
            OneSignal.Log(OneSignal.LOG_LEVEL.FATAL, "OneSignal AppId format is invalid.\nExample: 'b2f7f966-d8cc-11e4-bed1-df8f05be55ba'\n", th);
            return UNINITIALIZABLE_STATUS;
        }
    }

    static boolean hasFCMLibrary() {
        return true;
    }

    private static boolean hasGCMLibrary() {
        return true;
    }

    Integer checkForGooglePushLibrary() {
        boolean zHasFCMLibrary = hasFCMLibrary();
        boolean zHasGCMLibrary = hasGCMLibrary();
        if (!zHasFCMLibrary && !zHasGCMLibrary) {
            OneSignal.Log(OneSignal.LOG_LEVEL.FATAL, "The Firebase FCM library is missing! Please make sure to include it in your project.");
            return -4;
        }
        if (zHasGCMLibrary && !zHasFCMLibrary) {
            OneSignal.Log(OneSignal.LOG_LEVEL.WARN, "GCM Library detected, please upgrade to Firebase FCM library as GCM is deprecated!");
        }
        if (!zHasGCMLibrary || !zHasFCMLibrary) {
            return null;
        }
        OneSignal.Log(OneSignal.LOG_LEVEL.WARN, "Both GCM & FCM Libraries detected! Please remove the deprecated GCM library.");
        return null;
    }

    private static boolean hasWakefulBroadcastReceiver() {
        return true;
    }

    private static boolean hasNotificationManagerCompat() {
        return true;
    }

    private static boolean hasJobIntentService() {
        return true;
    }

    private Integer checkAndroidSupportLibrary(Context context) {
        boolean zHasWakefulBroadcastReceiver = hasWakefulBroadcastReceiver();
        boolean zHasNotificationManagerCompat = hasNotificationManagerCompat();
        if (!zHasWakefulBroadcastReceiver && !zHasNotificationManagerCompat) {
            OneSignal.Log(OneSignal.LOG_LEVEL.FATAL, "Could not find the Android Support Library. Please make sure it has been correctly added to your project.");
            return -3;
        }
        if (!zHasWakefulBroadcastReceiver || !zHasNotificationManagerCompat) {
            OneSignal.Log(OneSignal.LOG_LEVEL.FATAL, "The included Android Support Library is to old or incomplete. Please update to the 26.0.0 revision or newer.");
            return -5;
        }
        if (Build.VERSION.SDK_INT < 26 || getTargetSdkVersion(context) < 26 || hasJobIntentService()) {
            return null;
        }
        OneSignal.Log(OneSignal.LOG_LEVEL.FATAL, "The included Android Support Library is to old or incomplete. Please update to the 26.0.0 revision or newer.");
        return -5;
    }

    int getDeviceType() {
        try {
            Class.forName("com.amazon.device.messaging.ADM");
            return 2;
        } catch (ClassNotFoundException unused) {
            return 1;
        }
    }

    Integer getNetType() {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) OneSignal.appContext.getSystemService("connectivity")).getActiveNetworkInfo();
        if (activeNetworkInfo == null) {
            return null;
        }
        int type = activeNetworkInfo.getType();
        if (type == 1 || type == 9) {
            return 0;
        }
        return 1;
    }

    String getCarrierName() {
        try {
            String networkOperatorName = ((TelephonyManager) OneSignal.appContext.getSystemService("phone")).getNetworkOperatorName();
            if ("".equals(networkOperatorName)) {
                return null;
            }
            return networkOperatorName;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    static String getManifestMeta(Context context, String str) {
        try {
            return context.getPackageManager().getApplicationInfo(context.getPackageName(), 128).metaData.getString(str);
        } catch (Throwable th) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "", th);
            return null;
        }
    }

    static String getResourceString(Context context, String str, String str2) {
        Resources resources = context.getResources();
        int identifier = resources.getIdentifier(str, "string", context.getPackageName());
        return identifier != 0 ? resources.getString(identifier) : str2;
    }

    static String getCorrectedLanguage() {
        String language = Locale.getDefault().getLanguage();
        if (language.equals("iw")) {
            return "he";
        }
        if (language.equals("in")) {
            return "id";
        }
        if (language.equals("ji")) {
            return "yi";
        }
        if (!language.equals("zh")) {
            return language;
        }
        return language + "-" + Locale.getDefault().getCountry();
    }

    static boolean isValidEmail(String str) {
        if (str == null) {
            return false;
        }
        return Pattern.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$").matcher(str).matches();
    }

    static boolean areNotificationsEnabled(Context context) {
        try {
            return NotificationManagerCompat.from(OneSignal.appContext).areNotificationsEnabled();
        } catch (Throwable unused) {
            return true;
        }
    }

    static void runOnMainUIThread(Runnable runnable) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }

    static int getTargetSdkVersion(Context context) {
        try {
            return context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 15;
        }
    }

    static boolean isValidResourceName(String str) {
        return (str == null || str.matches("^[0-9]")) ? false : true;
    }

    static Uri getSoundUri(Context context, String str) {
        int identifier;
        Resources resources = context.getResources();
        String packageName = context.getPackageName();
        if (isValidResourceName(str) && (identifier = resources.getIdentifier(str, "raw", packageName)) != 0) {
            return Uri.parse("android.resource://" + packageName + "/" + identifier);
        }
        int identifier2 = resources.getIdentifier("onesignal_default_sound", "raw", packageName);
        if (identifier2 == 0) {
            return null;
        }
        return Uri.parse("android.resource://" + packageName + "/" + identifier2);
    }

    static long[] parseVibrationPattern(JSONObject jSONObject) {
        JSONArray jSONArray;
        try {
            Object objOpt = jSONObject.opt("vib_pt");
            if (objOpt instanceof String) {
                jSONArray = new JSONArray((String) objOpt);
            } else {
                jSONArray = (JSONArray) objOpt;
            }
            long[] jArr = new long[jSONArray.length()];
            for (int i = 0; i < jSONArray.length(); i++) {
                jArr[i] = jSONArray.optLong(i);
            }
            return jArr;
        } catch (JSONException unused) {
            return null;
        }
    }

    static String hexDigest(String str, String str2) throws Throwable {
        MessageDigest messageDigest = MessageDigest.getInstance(str2);
        messageDigest.update(str.getBytes("UTF-8"));
        byte[] bArrDigest = messageDigest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bArrDigest) {
            String hexString = Integer.toHexString(b & Constants.UNKNOWN);
            while (hexString.length() < 2) {
                hexString = "0" + hexString;
            }
            sb.append(hexString);
        }
        return sb.toString();
    }

    static void sleep(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
