package com.google.android.gms.common.internal;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.R;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.internal.zzbih;

/* JADX INFO: loaded from: classes.dex */
public final class zzu {
    private static final SimpleArrayMap<String, String> zzgfv = new SimpleArrayMap<>();

    private static String zzcn(Context context) {
        String packageName = context.getPackageName();
        try {
            return zzbih.zzdd(context).zzhc(packageName).toString();
        } catch (PackageManager.NameNotFoundException | NullPointerException unused) {
            String str = context.getApplicationInfo().name;
            return TextUtils.isEmpty(str) ? packageName : str;
        }
    }

    public static String zzco(Context context) {
        return context.getResources().getString(R.string.common_google_play_services_notification_channel_name);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:21:0x006b A[RETURN] */
    @android.support.annotation.Nullable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static java.lang.String zzf(android.content.Context r3, int r4) {
        /*
            android.content.res.Resources r0 = r3.getResources()
            r1 = 20
            if (r4 == r1) goto L81
            r1 = 0
            switch(r4) {
                case 1: goto L7a;
                case 2: goto L73;
                case 3: goto L6c;
                case 4: goto L6b;
                case 5: goto L5d;
                case 6: goto L6b;
                case 7: goto L4f;
                case 8: goto L4a;
                case 9: goto L45;
                case 10: goto L40;
                case 11: goto L3b;
                default: goto Lc;
            }
        Lc:
            switch(r4) {
                case 16: goto L36;
                case 17: goto L28;
                case 18: goto L6b;
                default: goto Lf;
            }
        Lf:
            java.lang.String r3 = "GoogleApiAvailability"
            r0 = 33
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>(r0)
            java.lang.String r0 = "Unexpected error code "
            r2.append(r0)
            r2.append(r4)
            java.lang.String r4 = r2.toString()
        L24:
            android.util.Log.e(r3, r4)
            return r1
        L28:
            java.lang.String r4 = "GoogleApiAvailability"
            java.lang.String r0 = "The specified account could not be signed in."
            android.util.Log.e(r4, r0)
            java.lang.String r4 = "common_google_play_services_sign_in_failed_title"
            java.lang.String r3 = zzs(r3, r4)
            return r3
        L36:
            java.lang.String r3 = "GoogleApiAvailability"
            java.lang.String r4 = "One of the API components you attempted to connect to is not available."
            goto L24
        L3b:
            java.lang.String r3 = "GoogleApiAvailability"
            java.lang.String r4 = "The application is not licensed to the user."
            goto L24
        L40:
            java.lang.String r3 = "GoogleApiAvailability"
            java.lang.String r4 = "Developer error occurred. Please see logs for detailed information"
            goto L24
        L45:
            java.lang.String r3 = "GoogleApiAvailability"
            java.lang.String r4 = "Google Play services is invalid. Cannot recover."
            goto L24
        L4a:
            java.lang.String r3 = "GoogleApiAvailability"
            java.lang.String r4 = "Internal error occurred. Please see logs for detailed information"
            goto L24
        L4f:
            java.lang.String r4 = "GoogleApiAvailability"
            java.lang.String r0 = "Network error occurred. Please retry request later."
            android.util.Log.e(r4, r0)
            java.lang.String r4 = "common_google_play_services_network_error_title"
            java.lang.String r3 = zzs(r3, r4)
            return r3
        L5d:
            java.lang.String r4 = "GoogleApiAvailability"
            java.lang.String r0 = "An invalid account was specified when connecting. Please provide a valid account."
            android.util.Log.e(r4, r0)
            java.lang.String r4 = "common_google_play_services_invalid_account_title"
            java.lang.String r3 = zzs(r3, r4)
            return r3
        L6b:
            return r1
        L6c:
            int r3 = com.google.android.gms.R.string.common_google_play_services_enable_title
            java.lang.String r3 = r0.getString(r3)
            return r3
        L73:
            int r3 = com.google.android.gms.R.string.common_google_play_services_update_title
            java.lang.String r3 = r0.getString(r3)
            return r3
        L7a:
            int r3 = com.google.android.gms.R.string.common_google_play_services_install_title
            java.lang.String r3 = r0.getString(r3)
            return r3
        L81:
            java.lang.String r4 = "GoogleApiAvailability"
            java.lang.String r0 = "The current user profile is restricted and could not use authenticated features."
            android.util.Log.e(r4, r0)
            java.lang.String r4 = "common_google_play_services_restricted_profile_title"
            java.lang.String r3 = zzs(r3, r4)
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.common.internal.zzu.zzf(android.content.Context, int):java.lang.String");
    }

    @NonNull
    public static String zzg(Context context, int i) {
        String strZzs = i == 6 ? zzs(context, "common_google_play_services_resolution_required_title") : zzf(context, i);
        return strZzs == null ? context.getResources().getString(R.string.common_google_play_services_notification_ticker) : strZzs;
    }

    @NonNull
    public static String zzh(Context context, int i) {
        Resources resources = context.getResources();
        String strZzcn = zzcn(context);
        if (i == 5) {
            return zzl(context, "common_google_play_services_invalid_account_text", strZzcn);
        }
        if (i == 7) {
            return zzl(context, "common_google_play_services_network_error_text", strZzcn);
        }
        if (i == 9) {
            return resources.getString(R.string.common_google_play_services_unsupported_text, strZzcn);
        }
        if (i == 20) {
            return zzl(context, "common_google_play_services_restricted_profile_text", strZzcn);
        }
        switch (i) {
            case 1:
                return resources.getString(R.string.common_google_play_services_install_text, strZzcn);
            case 2:
                return com.google.android.gms.common.util.zzj.zzcv(context) ? resources.getString(R.string.common_google_play_services_wear_update_text) : resources.getString(R.string.common_google_play_services_update_text, strZzcn);
            case 3:
                return resources.getString(R.string.common_google_play_services_enable_text, strZzcn);
            default:
                switch (i) {
                    case 16:
                        return zzl(context, "common_google_play_services_api_unavailable_text", strZzcn);
                    case 17:
                        return zzl(context, "common_google_play_services_sign_in_failed_text", strZzcn);
                    case 18:
                        return resources.getString(R.string.common_google_play_services_updating_text, strZzcn);
                    default:
                        return resources.getString(R.string.common_google_play_services_unknown_issue, strZzcn);
                }
        }
    }

    @NonNull
    public static String zzi(Context context, int i) {
        return i == 6 ? zzl(context, "common_google_play_services_resolution_required_text", zzcn(context)) : zzh(context, i);
    }

    @NonNull
    public static String zzj(Context context, int i) {
        int i2;
        Resources resources = context.getResources();
        switch (i) {
            case 1:
                i2 = R.string.common_google_play_services_install_button;
                break;
            case 2:
                i2 = R.string.common_google_play_services_update_button;
                break;
            case 3:
                i2 = R.string.common_google_play_services_enable_button;
                break;
            default:
                i2 = android.R.string.ok;
                break;
        }
        return resources.getString(i2);
    }

    private static String zzl(Context context, String str, String str2) {
        Resources resources = context.getResources();
        String strZzs = zzs(context, str);
        if (strZzs == null) {
            strZzs = resources.getString(R.string.common_google_play_services_unknown_issue);
        }
        return String.format(resources.getConfiguration().locale, strZzs, str2);
    }

    @Nullable
    private static String zzs(Context context, String str) {
        synchronized (zzgfv) {
            String str2 = zzgfv.get(str);
            if (str2 != null) {
                return str2;
            }
            Resources remoteResource = GooglePlayServicesUtil.getRemoteResource(context);
            if (remoteResource == null) {
                return null;
            }
            int identifier = remoteResource.getIdentifier(str, "string", "com.google.android.gms");
            if (identifier == 0) {
                String strValueOf = String.valueOf(str);
                Log.w("GoogleApiAvailability", strValueOf.length() != 0 ? "Missing resource: ".concat(strValueOf) : new String("Missing resource: "));
                return null;
            }
            String string = remoteResource.getString(identifier);
            if (!TextUtils.isEmpty(string)) {
                zzgfv.put(str, string);
                return string;
            }
            String strValueOf2 = String.valueOf(str);
            Log.w("GoogleApiAvailability", strValueOf2.length() != 0 ? "Got empty resource: ".concat(strValueOf2) : new String("Got empty resource: "));
            return null;
        }
    }
}
