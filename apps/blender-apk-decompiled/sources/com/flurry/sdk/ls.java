package com.flurry.sdk;

import android.content.Context;

/* JADX INFO: loaded from: classes.dex */
public class ls {
    private static final String a = "ls";
    private static String b = "com.google.android.gms.common.GoogleApiAvailability";
    private static String c = "com.google.android.gms.common.GooglePlayServicesUtil";
    private static String d = "com.google.android.gms.ads.identifier.AdvertisingIdClient";

    public static boolean a(Context context) {
        try {
            try {
                return a(context, b);
            } catch (Exception e) {
                kf.b(a, "GOOGLE PLAY SERVICES EXCEPTION: " + e.getMessage());
                kf.b(a, "There is a problem with the Google Play Services library, which is required for Android Advertising ID support. The Google Play Services library should be integrated in any app shipping in the Play Store that uses analytics or advertising.");
                return false;
            }
        } catch (Exception unused) {
            return a(context, c);
        }
    }

    private static boolean a(Context context, String str) throws Exception {
        Object objA = lu.a(null, "isGooglePlayServicesAvailable").a(Class.forName(str)).a(Context.class, context).a();
        return objA != null && ((Integer) objA).intValue() == 0;
    }

    public static jo b(Context context) {
        if (context == null) {
            return null;
        }
        try {
            Object objA = lu.a(null, "getAdvertisingIdInfo").a(Class.forName(d)).a(Context.class, context).a();
            return new jo(a(objA), b(objA));
        } catch (Exception e) {
            kf.b(a, "GOOGLE PLAY SERVICES ERROR: " + e.getMessage());
            kf.b(a, "There is a problem with the Google Play Services library, which is required for Android Advertising ID support. The Google Play Services library should be integrated in any app shipping in the Play Store that uses analytics or advertising.");
            return null;
        }
    }

    private static String a(Object obj) {
        try {
            return (String) lu.a(obj, "getId").a();
        } catch (Exception e) {
            kf.b(a, "GOOGLE PLAY SERVICES ERROR: " + e.getMessage());
            kf.b(a, "There is a problem with the Google Play Services library, which is required for Android Advertising ID support. The Google Play Services library should be integrated in any app shipping in the Play Store that uses analytics or advertising.");
            return null;
        }
    }

    private static boolean b(Object obj) {
        try {
            Boolean bool = (Boolean) lu.a(obj, "isLimitAdTrackingEnabled").a();
            if (bool != null) {
                return bool.booleanValue();
            }
            return false;
        } catch (Exception e) {
            kf.b(a, "GOOGLE PLAY SERVICES ERROR: " + e.getMessage());
            kf.b(a, "There is a problem with the Google Play Services library, which is required for Android Advertising ID support. The Google Play Services library should be integrated in any app shipping in the Play Store that uses analytics or advertising.");
            return false;
        }
    }
}
