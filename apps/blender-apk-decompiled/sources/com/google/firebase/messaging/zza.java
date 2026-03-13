package com.google.firebase.messaging;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.common.util.zzs;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.MissingFormatArgumentException;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONException;

/* JADX INFO: loaded from: classes.dex */
final class zza {
    private static zza zzolt;
    private final Context mContext;
    private Bundle zzgco;
    private Method zzolu;
    private Method zzolv;
    private final AtomicInteger zzolw = new AtomicInteger((int) SystemClock.elapsedRealtime());

    private zza(Context context) {
        this.mContext = context.getApplicationContext();
    }

    @TargetApi(26)
    private final Notification zza(CharSequence charSequence, String str, int i, Integer num, Uri uri, PendingIntent pendingIntent, PendingIntent pendingIntent2, String str2) {
        Notification.Builder smallIcon = new Notification.Builder(this.mContext).setAutoCancel(true).setSmallIcon(i);
        if (!TextUtils.isEmpty(charSequence)) {
            smallIcon.setContentTitle(charSequence);
        }
        if (!TextUtils.isEmpty(str)) {
            smallIcon.setContentText(str);
            smallIcon.setStyle(new Notification.BigTextStyle().bigText(str));
        }
        if (num != null) {
            smallIcon.setColor(num.intValue());
        }
        if (uri != null) {
            smallIcon.setSound(uri);
        }
        if (pendingIntent != null) {
            smallIcon.setContentIntent(pendingIntent);
        }
        if (pendingIntent2 != null) {
            smallIcon.setDeleteIntent(pendingIntent2);
        }
        if (str2 != null) {
            if (this.zzolu == null) {
                this.zzolu = zzrx("setChannelId");
            }
            if (this.zzolu == null) {
                this.zzolu = zzrx("setChannel");
            }
            if (this.zzolu == null) {
                Log.e("FirebaseMessaging", "Error while setting the notification channel");
            } else {
                try {
                    this.zzolu.invoke(smallIcon, str2);
                } catch (IllegalAccessException | IllegalArgumentException | SecurityException | InvocationTargetException e) {
                    Log.e("FirebaseMessaging", "Error while setting the notification channel", e);
                }
            }
        }
        return smallIcon.build();
    }

    private static void zza(Intent intent, Bundle bundle) {
        for (String str : bundle.keySet()) {
            if (str.startsWith("google.c.a.") || str.equals("from")) {
                intent.putExtra(str, bundle.getString(str));
            }
        }
    }

    static boolean zzai(Bundle bundle) {
        return "1".equals(zzd(bundle, "gcm.n.e")) || zzd(bundle, "gcm.n.icon") != null;
    }

    @Nullable
    static Uri zzaj(@NonNull Bundle bundle) {
        String strZzd = zzd(bundle, "gcm.n.link_android");
        if (TextUtils.isEmpty(strZzd)) {
            strZzd = zzd(bundle, "gcm.n.link");
        }
        if (TextUtils.isEmpty(strZzd)) {
            return null;
        }
        return Uri.parse(strZzd);
    }

    static String zzak(Bundle bundle) {
        String strZzd = zzd(bundle, "gcm.n.sound2");
        return TextUtils.isEmpty(strZzd) ? zzd(bundle, "gcm.n.sound") : strZzd;
    }

    private final Bundle zzawf() throws PackageManager.NameNotFoundException {
        if (this.zzgco != null) {
            return this.zzgco;
        }
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = this.mContext.getPackageManager().getApplicationInfo(this.mContext.getPackageName(), 128);
        } catch (PackageManager.NameNotFoundException unused) {
        }
        if (applicationInfo == null || applicationInfo.metaData == null) {
            return Bundle.EMPTY;
        }
        this.zzgco = applicationInfo.metaData;
        return this.zzgco;
    }

    static String zzd(Bundle bundle, String str) {
        String string = bundle.getString(str);
        return string == null ? bundle.getString(str.replace("gcm.n.", "gcm.notification.")) : string;
    }

    static synchronized zza zzfc(Context context) {
        if (zzolt == null) {
            zzolt = new zza(context);
        }
        return zzolt;
    }

    static String zzh(Bundle bundle, String str) {
        String strValueOf = String.valueOf(str);
        String strValueOf2 = String.valueOf("_loc_key");
        return zzd(bundle, strValueOf2.length() != 0 ? strValueOf.concat(strValueOf2) : new String(strValueOf));
    }

    /* JADX WARN: Multi-variable type inference failed */
    static Object[] zzi(Bundle bundle, String str) {
        String strValueOf = String.valueOf(str);
        String strValueOf2 = String.valueOf("_loc_args");
        String strZzd = zzd(bundle, strValueOf2.length() != 0 ? strValueOf.concat(strValueOf2) : new String(strValueOf));
        if (TextUtils.isEmpty(strZzd)) {
            return null;
        }
        try {
            JSONArray jSONArray = new JSONArray(strZzd);
            String[] strArr = new String[jSONArray.length()];
            for (int i = 0; i < strArr.length; i++) {
                strArr[i] = jSONArray.opt(i);
            }
            return strArr;
        } catch (JSONException unused) {
            String strValueOf3 = String.valueOf(str);
            String strValueOf4 = String.valueOf("_loc_args");
            String strSubstring = (strValueOf4.length() != 0 ? strValueOf3.concat(strValueOf4) : new String(strValueOf3)).substring(6);
            StringBuilder sb = new StringBuilder(String.valueOf(strSubstring).length() + 41 + String.valueOf(strZzd).length());
            sb.append("Malformed ");
            sb.append(strSubstring);
            sb.append(": ");
            sb.append(strZzd);
            sb.append("  Default value will be used.");
            Log.w("FirebaseMessaging", sb.toString());
            return null;
        }
    }

    @TargetApi(26)
    private final boolean zzit(int i) {
        if (Build.VERSION.SDK_INT != 26) {
            return true;
        }
        try {
            if (!(this.mContext.getResources().getDrawable(i, null) instanceof AdaptiveIconDrawable)) {
                return true;
            }
            StringBuilder sb = new StringBuilder(77);
            sb.append("Adaptive icons cannot be used in notifications. Ignoring icon id: ");
            sb.append(i);
            Log.e("FirebaseMessaging", sb.toString());
            return false;
        } catch (Resources.NotFoundException unused) {
            return false;
        }
    }

    private final String zzj(Bundle bundle, String str) {
        String strZzd = zzd(bundle, str);
        if (!TextUtils.isEmpty(strZzd)) {
            return strZzd;
        }
        String strZzh = zzh(bundle, str);
        if (TextUtils.isEmpty(strZzh)) {
            return null;
        }
        Resources resources = this.mContext.getResources();
        int identifier = resources.getIdentifier(strZzh, "string", this.mContext.getPackageName());
        if (identifier == 0) {
            String strValueOf = String.valueOf(str);
            String strValueOf2 = String.valueOf("_loc_key");
            String strSubstring = (strValueOf2.length() != 0 ? strValueOf.concat(strValueOf2) : new String(strValueOf)).substring(6);
            StringBuilder sb = new StringBuilder(String.valueOf(strSubstring).length() + 49 + String.valueOf(strZzh).length());
            sb.append(strSubstring);
            sb.append(" resource not found: ");
            sb.append(strZzh);
            sb.append(" Default value will be used.");
            Log.w("FirebaseMessaging", sb.toString());
            return null;
        }
        Object[] objArrZzi = zzi(bundle, str);
        if (objArrZzi == null) {
            return resources.getString(identifier);
        }
        try {
            return resources.getString(identifier, objArrZzi);
        } catch (MissingFormatArgumentException e) {
            String string = Arrays.toString(objArrZzi);
            StringBuilder sb2 = new StringBuilder(String.valueOf(strZzh).length() + 58 + String.valueOf(string).length());
            sb2.append("Missing format argument for ");
            sb2.append(strZzh);
            sb2.append(": ");
            sb2.append(string);
            sb2.append(" Default value will be used.");
            Log.w("FirebaseMessaging", sb2.toString(), e);
            return null;
        }
    }

    @TargetApi(26)
    private static Method zzrx(String str) {
        try {
            return Notification.Builder.class.getMethod(str, String.class);
        } catch (NoSuchMethodException | SecurityException unused) {
            return null;
        }
    }

    private final Integer zzry(String str) {
        if (Build.VERSION.SDK_INT < 21) {
            return null;
        }
        if (!TextUtils.isEmpty(str)) {
            try {
                return Integer.valueOf(Color.parseColor(str));
            } catch (IllegalArgumentException unused) {
                StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 54);
                sb.append("Color ");
                sb.append(str);
                sb.append(" not valid. Notification will use default color.");
                Log.w("FirebaseMessaging", sb.toString());
            }
        }
        int i = zzawf().getInt("com.google.firebase.messaging.default_notification_color", 0);
        if (i != 0) {
            try {
                return Integer.valueOf(ContextCompat.getColor(this.mContext, i));
            } catch (Resources.NotFoundException unused2) {
                Log.w("FirebaseMessaging", "Cannot find the color resource referenced in AndroidManifest.");
            }
        }
        return null;
    }

    @TargetApi(26)
    private final String zzrz(String str) {
        String str2;
        String str3;
        if (!zzs.isAtLeastO()) {
            return null;
        }
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        try {
            if (this.zzolv == null) {
                this.zzolv = notificationManager.getClass().getMethod("getNotificationChannel", String.class);
            }
            if (!TextUtils.isEmpty(str)) {
                if (this.zzolv.invoke(notificationManager, str) != null) {
                    return str;
                }
                StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 122);
                sb.append("Notification Channel requested (");
                sb.append(str);
                sb.append(") has not been created by the app. Manifest configuration, or default, value will be used.");
                Log.w("FirebaseMessaging", sb.toString());
            }
            String string = zzawf().getString("com.google.firebase.messaging.default_notification_channel_id");
            if (TextUtils.isEmpty(string)) {
                str2 = "FirebaseMessaging";
                str3 = "Missing Default Notification Channel metadata in AndroidManifest. Default value will be used.";
            } else {
                if (this.zzolv.invoke(notificationManager, string) != null) {
                    return string;
                }
                str2 = "FirebaseMessaging";
                str3 = "Notification Channel set in AndroidManifest.xml has not been created by the app. Default value will be used.";
            }
            Log.w(str2, str3);
            if (this.zzolv.invoke(notificationManager, "fcm_fallback_notification_channel") != null) {
                return "fcm_fallback_notification_channel";
            }
            Class<?> cls = Class.forName("android.app.NotificationChannel");
            notificationManager.getClass().getMethod("createNotificationChannel", cls).invoke(notificationManager, cls.getConstructor(String.class, CharSequence.class, Integer.TYPE).newInstance("fcm_fallback_notification_channel", this.mContext.getString(com.google.android.gms.R.string.fcm_fallback_notification_channel_label), 3));
            return "fcm_fallback_notification_channel";
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | LinkageError | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            Log.e("FirebaseMessaging", "Error while setting the notification channel", e);
            return null;
        }
    }

    private final PendingIntent zzu(Bundle bundle) {
        Intent launchIntentForPackage;
        String strZzd = zzd(bundle, "gcm.n.click_action");
        if (TextUtils.isEmpty(strZzd)) {
            Uri uriZzaj = zzaj(bundle);
            if (uriZzaj != null) {
                launchIntentForPackage = new Intent("android.intent.action.VIEW");
                launchIntentForPackage.setPackage(this.mContext.getPackageName());
                launchIntentForPackage.setData(uriZzaj);
            } else {
                launchIntentForPackage = this.mContext.getPackageManager().getLaunchIntentForPackage(this.mContext.getPackageName());
                if (launchIntentForPackage == null) {
                    Log.w("FirebaseMessaging", "No activity found to launch app");
                }
            }
        } else {
            launchIntentForPackage = new Intent(strZzd);
            launchIntentForPackage.setPackage(this.mContext.getPackageName());
            launchIntentForPackage.setFlags(268435456);
        }
        if (launchIntentForPackage == null) {
            return null;
        }
        launchIntentForPackage.addFlags(67108864);
        Bundle bundle2 = new Bundle(bundle);
        FirebaseMessagingService.zzr(bundle2);
        launchIntentForPackage.putExtras(bundle2);
        for (String str : bundle2.keySet()) {
            if (str.startsWith("gcm.n.") || str.startsWith("gcm.notification.")) {
                launchIntentForPackage.removeExtra(str);
            }
        }
        return PendingIntent.getActivity(this.mContext, this.zzolw.incrementAndGet(), launchIntentForPackage, 1073741824);
    }

    /* JADX WARN: Removed duplicated region for block: B:20:0x005c A[EDGE_INSN: B:90:0x005c->B:20:0x005c BREAK  A[LOOP:0: B:13:0x0044->B:91:?]] */
    /* JADX WARN: Removed duplicated region for block: B:43:0x00f7  */
    /* JADX WARN: Removed duplicated region for block: B:47:0x0107  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    final boolean zzt(android.os.Bundle r13) {
        /*
            Method dump skipped, instruction units count: 614
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.firebase.messaging.zza.zzt(android.os.Bundle):boolean");
    }
}
