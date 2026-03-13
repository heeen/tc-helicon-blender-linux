package com.onesignal;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;
import com.onesignal.AndroidSupportV4Compat;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalDbContract;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
class GenerateNotification {
    private static Resources contextResources;
    private static Context currentContext;
    private static Class<?> notificationOpenedClass;
    private static boolean openerIsBroadcast;
    private static String packageName;

    private static int convertOSToAndroidPriority(int i) {
        if (i > 9) {
            return 2;
        }
        if (i > 7) {
            return 1;
        }
        if (i > 4) {
            return 0;
        }
        return i > 2 ? -1 : -2;
    }

    GenerateNotification() {
    }

    private static class OneSignalNotificationBuilder {
        NotificationCompat.Builder compatBuilder;
        boolean hasLargeIcon;

        private OneSignalNotificationBuilder() {
        }
    }

    private static void setStatics(Context context) {
        currentContext = context;
        packageName = currentContext.getPackageName();
        contextResources = currentContext.getResources();
        PackageManager packageManager = currentContext.getPackageManager();
        Intent intent = new Intent(currentContext, (Class<?>) NotificationOpenedReceiver.class);
        intent.setPackage(currentContext.getPackageName());
        if (packageManager.queryBroadcastReceivers(intent, 0).size() > 0) {
            openerIsBroadcast = true;
            notificationOpenedClass = NotificationOpenedReceiver.class;
        } else {
            notificationOpenedClass = NotificationOpenedActivity.class;
        }
    }

    static void fromJsonPayload(NotificationGenerationJob notificationGenerationJob) {
        setStatics(notificationGenerationJob.context);
        if (!notificationGenerationJob.restoring && notificationGenerationJob.showAsAlert && ActivityLifecycleHandler.curActivity != null) {
            showNotificationAsAlert(notificationGenerationJob.jsonPayload, ActivityLifecycleHandler.curActivity, notificationGenerationJob.getAndroidId().intValue());
        } else {
            showNotification(notificationGenerationJob);
        }
    }

    private static void showNotificationAsAlert(final JSONObject jSONObject, final Activity activity, final int i) {
        activity.runOnUiThread(new Runnable() { // from class: com.onesignal.GenerateNotification.1
            @Override // java.lang.Runnable
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(GenerateNotification.getTitle(jSONObject));
                builder.setMessage(jSONObject.optString("alert"));
                ArrayList arrayList = new ArrayList();
                final ArrayList arrayList2 = new ArrayList();
                GenerateNotification.addAlertButtons(activity, jSONObject, arrayList, arrayList2);
                final Intent newBaseIntent = GenerateNotification.getNewBaseIntent(i);
                newBaseIntent.putExtra("action_button", true);
                newBaseIntent.putExtra("from_alert", true);
                newBaseIntent.putExtra("onesignal_data", jSONObject.toString());
                if (jSONObject.has("grp")) {
                    newBaseIntent.putExtra("grp", jSONObject.optString("grp"));
                }
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() { // from class: com.onesignal.GenerateNotification.1.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i2) throws Throwable {
                        int i3 = i2 + 3;
                        if (arrayList2.size() > 1) {
                            try {
                                JSONObject jSONObject2 = new JSONObject(jSONObject.toString());
                                jSONObject2.put("actionSelected", arrayList2.get(i3));
                                newBaseIntent.putExtra("onesignal_data", jSONObject2.toString());
                                NotificationOpenedProcessor.processIntent(activity, newBaseIntent);
                                return;
                            } catch (Throwable unused) {
                                return;
                            }
                        }
                        NotificationOpenedProcessor.processIntent(activity, newBaseIntent);
                    }
                };
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.onesignal.GenerateNotification.1.2
                    @Override // android.content.DialogInterface.OnCancelListener
                    public void onCancel(DialogInterface dialogInterface) throws Throwable {
                        NotificationOpenedProcessor.processIntent(activity, newBaseIntent);
                    }
                });
                for (int i2 = 0; i2 < arrayList.size(); i2++) {
                    if (i2 == 0) {
                        builder.setNeutralButton((CharSequence) arrayList.get(i2), onClickListener);
                    } else if (i2 == 1) {
                        builder.setNegativeButton((CharSequence) arrayList.get(i2), onClickListener);
                    } else if (i2 == 2) {
                        builder.setPositiveButton((CharSequence) arrayList.get(i2), onClickListener);
                    }
                }
                AlertDialog alertDialogCreate = builder.create();
                alertDialogCreate.setCanceledOnTouchOutside(false);
                alertDialogCreate.show();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static CharSequence getTitle(JSONObject jSONObject) {
        String strOptString = jSONObject.optString(OneSignalDbContract.NotificationTable.COLUMN_NAME_TITLE, null);
        return strOptString != null ? strOptString : currentContext.getPackageManager().getApplicationLabel(currentContext.getApplicationInfo());
    }

    private static PendingIntent getNewActionPendingIntent(int i, Intent intent) {
        if (openerIsBroadcast) {
            return PendingIntent.getBroadcast(currentContext, i, intent, 134217728);
        }
        return PendingIntent.getActivity(currentContext, i, intent, 134217728);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Intent getNewBaseIntent(int i) {
        Intent intentPutExtra = new Intent(currentContext, notificationOpenedClass).putExtra("notificationId", i);
        return openerIsBroadcast ? intentPutExtra : intentPutExtra.addFlags(603979776);
    }

    private static Intent getNewBaseDeleteIntent(int i) {
        Intent intentPutExtra = new Intent(currentContext, notificationOpenedClass).putExtra("notificationId", i).putExtra(OneSignalDbContract.NotificationTable.COLUMN_NAME_DISMISSED, true);
        return openerIsBroadcast ? intentPutExtra : intentPutExtra.addFlags(402718720);
    }

    private static OneSignalNotificationBuilder getBaseOneSignalNotificationBuilder(NotificationGenerationJob notificationGenerationJob) {
        NotificationCompat.Builder builder;
        JSONObject jSONObject = notificationGenerationJob.jsonPayload;
        OneSignalNotificationBuilder oneSignalNotificationBuilder = new OneSignalNotificationBuilder();
        try {
            builder = new NotificationCompat.Builder(currentContext, NotificationChannelManager.createNotificationChannel(notificationGenerationJob));
        } catch (Throwable unused) {
            builder = new NotificationCompat.Builder(currentContext);
        }
        String strOptString = jSONObject.optString("alert", null);
        builder.setAutoCancel(true).setSmallIcon(getSmallIconId(jSONObject)).setStyle(new NotificationCompat.BigTextStyle().bigText(strOptString)).setContentText(strOptString).setTicker(strOptString);
        if (Build.VERSION.SDK_INT < 24 || !jSONObject.optString(OneSignalDbContract.NotificationTable.COLUMN_NAME_TITLE).equals("")) {
            builder.setContentTitle(getTitle(jSONObject));
        }
        try {
            BigInteger accentColor = getAccentColor(jSONObject);
            if (accentColor != null) {
                builder.setColor(accentColor.intValue());
            }
        } catch (Throwable unused2) {
        }
        try {
            builder.setVisibility(jSONObject.has("vis") ? Integer.parseInt(jSONObject.optString("vis")) : 1);
        } catch (Throwable unused3) {
        }
        Bitmap largeIcon = getLargeIcon(jSONObject);
        if (largeIcon != null) {
            oneSignalNotificationBuilder.hasLargeIcon = true;
            builder.setLargeIcon(largeIcon);
        }
        Bitmap bitmap = getBitmap(jSONObject.optString("bicon", null));
        if (bitmap != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).setSummaryText(strOptString));
        }
        if (notificationGenerationJob.shownTimeStamp != null) {
            try {
                builder.setWhen(notificationGenerationJob.shownTimeStamp.longValue() * 1000);
            } catch (Throwable unused4) {
            }
        }
        setAlertnessOptions(jSONObject, builder);
        oneSignalNotificationBuilder.compatBuilder = builder;
        return oneSignalNotificationBuilder;
    }

    private static void setAlertnessOptions(JSONObject jSONObject, NotificationCompat.Builder builder) {
        int iConvertOSToAndroidPriority = convertOSToAndroidPriority(jSONObject.optInt("pri", 6));
        builder.setPriority(iConvertOSToAndroidPriority);
        if (iConvertOSToAndroidPriority < 0) {
            return;
        }
        int i = 4;
        if (jSONObject.has("ledc") && jSONObject.optInt("led", 1) == 1) {
            try {
                builder.setLights(new BigInteger(jSONObject.optString("ledc"), 16).intValue(), 2000, 5000);
                i = 0;
            } catch (Throwable unused) {
            }
        }
        if (OneSignal.getVibrate(currentContext) && jSONObject.optInt("vib", 1) == 1) {
            if (jSONObject.has("vib_pt")) {
                long[] vibrationPattern = OSUtils.parseVibrationPattern(jSONObject);
                if (vibrationPattern != null) {
                    builder.setVibrate(vibrationPattern);
                }
            } else {
                i |= 2;
            }
        }
        if (isSoundEnabled(jSONObject)) {
            Uri soundUri = OSUtils.getSoundUri(currentContext, jSONObject.optString("sound", null));
            if (soundUri != null) {
                builder.setSound(soundUri);
            } else {
                i |= 1;
            }
        }
        builder.setDefaults(i);
    }

    private static void removeNotifyOptions(NotificationCompat.Builder builder) {
        builder.setOnlyAlertOnce(true).setDefaults(0).setSound(null).setVibrate(null).setTicker(null);
    }

    private static void showNotification(NotificationGenerationJob notificationGenerationJob) throws Throwable {
        Notification notificationBuild;
        SecureRandom secureRandom = new SecureRandom();
        int iIntValue = notificationGenerationJob.getAndroidId().intValue();
        JSONObject jSONObject = notificationGenerationJob.jsonPayload;
        String strOptString = jSONObject.optString("grp", null);
        OneSignalNotificationBuilder baseOneSignalNotificationBuilder = getBaseOneSignalNotificationBuilder(notificationGenerationJob);
        NotificationCompat.Builder builder = baseOneSignalNotificationBuilder.compatBuilder;
        addNotificationActionButtons(jSONObject, builder, iIntValue, null);
        try {
            addBackgroundImage(jSONObject, builder);
        } catch (Throwable th) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Could not set background notification image!", th);
        }
        applyNotificationExtender(notificationGenerationJob, builder);
        if (notificationGenerationJob.restoring) {
            removeNotifyOptions(builder);
        }
        NotificationLimitManager.clearOldestOverLimit(currentContext, strOptString != null ? 2 : 1);
        if (strOptString != null) {
            builder.setContentIntent(getNewActionPendingIntent(secureRandom.nextInt(), getNewBaseIntent(iIntValue).putExtra("onesignal_data", jSONObject.toString()).putExtra("grp", strOptString)));
            builder.setDeleteIntent(getNewActionPendingIntent(secureRandom.nextInt(), getNewBaseDeleteIntent(iIntValue).putExtra("grp", strOptString)));
            builder.setGroup(strOptString);
            try {
                builder.setGroupAlertBehavior(1);
            } catch (Throwable unused) {
            }
            notificationBuild = createSingleNotificationBeforeSummaryBuilder(notificationGenerationJob, builder);
            createSummaryNotification(notificationGenerationJob, baseOneSignalNotificationBuilder);
        } else {
            builder.setContentIntent(getNewActionPendingIntent(secureRandom.nextInt(), getNewBaseIntent(iIntValue).putExtra("onesignal_data", jSONObject.toString())));
            builder.setDeleteIntent(getNewActionPendingIntent(secureRandom.nextInt(), getNewBaseDeleteIntent(iIntValue)));
            notificationBuild = builder.build();
        }
        if (strOptString == null || Build.VERSION.SDK_INT > 17) {
            addXiaomiSettings(baseOneSignalNotificationBuilder, notificationBuild);
            NotificationManagerCompat.from(currentContext).notify(iIntValue, notificationBuild);
        }
    }

    private static void applyNotificationExtender(NotificationGenerationJob notificationGenerationJob, NotificationCompat.Builder builder) {
        if (notificationGenerationJob.overrideSettings == null || notificationGenerationJob.overrideSettings.extender == null) {
            return;
        }
        try {
            Field declaredField = NotificationCompat.Builder.class.getDeclaredField("mNotification");
            declaredField.setAccessible(true);
            Notification notification = (Notification) declaredField.get(builder);
            notificationGenerationJob.orgFlags = Integer.valueOf(notification.flags);
            notificationGenerationJob.orgSound = notification.sound;
            builder.extend(notificationGenerationJob.overrideSettings.extender);
            Notification notification2 = (Notification) declaredField.get(builder);
            Field declaredField2 = NotificationCompat.Builder.class.getDeclaredField("mContentText");
            declaredField2.setAccessible(true);
            CharSequence charSequence = (CharSequence) declaredField2.get(builder);
            Field declaredField3 = NotificationCompat.Builder.class.getDeclaredField("mContentTitle");
            declaredField3.setAccessible(true);
            CharSequence charSequence2 = (CharSequence) declaredField3.get(builder);
            notificationGenerationJob.overriddenBodyFromExtender = charSequence;
            notificationGenerationJob.overriddenTitleFromExtender = charSequence2;
            if (notificationGenerationJob.restoring) {
                return;
            }
            notificationGenerationJob.overriddenFlags = Integer.valueOf(notification2.flags);
            notificationGenerationJob.overriddenSound = notification2.sound;
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private static Notification createSingleNotificationBeforeSummaryBuilder(NotificationGenerationJob notificationGenerationJob, NotificationCompat.Builder builder) {
        boolean z = Build.VERSION.SDK_INT > 17 && Build.VERSION.SDK_INT < 24 && !notificationGenerationJob.restoring;
        if (z && notificationGenerationJob.overriddenSound != null && !notificationGenerationJob.overriddenSound.equals(notificationGenerationJob.orgSound)) {
            builder.setSound(null);
        }
        Notification notificationBuild = builder.build();
        if (z) {
            builder.setSound(notificationGenerationJob.overriddenSound);
        }
        return notificationBuild;
    }

    private static void addXiaomiSettings(OneSignalNotificationBuilder oneSignalNotificationBuilder, Notification notification) {
        if (oneSignalNotificationBuilder.hasLargeIcon) {
            try {
                Object objNewInstance = Class.forName("android.app.MiuiNotification").newInstance();
                Field declaredField = objNewInstance.getClass().getDeclaredField("customizedIcon");
                declaredField.setAccessible(true);
                declaredField.set(objNewInstance, true);
                Field field = notification.getClass().getField("extraNotification");
                field.setAccessible(true);
                field.set(notification, objNewInstance);
            } catch (Throwable unused) {
            }
        }
    }

    static void updateSummaryNotification(NotificationGenerationJob notificationGenerationJob) {
        setStatics(notificationGenerationJob.context);
        createSummaryNotification(notificationGenerationJob, null);
    }

    /* JADX WARN: Type inference fix 'apply assigned field type' failed
    java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$PrimitiveArg
    	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:593)
    	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
    	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
     */
    private static void createSummaryNotification(NotificationGenerationJob notificationGenerationJob, OneSignalNotificationBuilder oneSignalNotificationBuilder) throws Throwable {
        Cursor cursorQuery;
        ArrayList arrayList;
        Integer numValueOf;
        Notification notificationBuild;
        CharSequence charSequenceReplace;
        String str;
        String str2;
        boolean z = notificationGenerationJob.restoring;
        JSONObject jSONObject = notificationGenerationJob.jsonPayload;
        String strOptString = jSONObject.optString("grp", null);
        SecureRandom secureRandom = new SecureRandom();
        PendingIntent newActionPendingIntent = getNewActionPendingIntent(secureRandom.nextInt(), getNewBaseDeleteIntent(0).putExtra("summary", strOptString));
        OneSignalDbHelper oneSignalDbHelper = OneSignalDbHelper.getInstance(currentContext);
        try {
            SQLiteDatabase readableDbWithRetries = oneSignalDbHelper.getReadableDbWithRetries();
            String[] strArr = {OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID, OneSignalDbContract.NotificationTable.COLUMN_NAME_FULL_DATA, OneSignalDbContract.NotificationTable.COLUMN_NAME_IS_SUMMARY, OneSignalDbContract.NotificationTable.COLUMN_NAME_TITLE, OneSignalDbContract.NotificationTable.COLUMN_NAME_MESSAGE};
            String str3 = "group_id = ? AND dismissed = 0 AND opened = 0";
            String[] strArr2 = {strOptString};
            if (!z && notificationGenerationJob.getAndroidId().intValue() != -1) {
                str3 = "group_id = ? AND dismissed = 0 AND opened = 0 AND android_notification_id <> " + notificationGenerationJob.getAndroidId();
            }
            int i = 1;
            try {
                cursorQuery = readableDbWithRetries.query(OneSignalDbContract.NotificationTable.TABLE_NAME, strArr, str3, strArr2, null, null, "_id DESC");
                try {
                    if (cursorQuery.moveToFirst()) {
                        arrayList = new ArrayList();
                        String string = null;
                        numValueOf = null;
                        while (true) {
                            if (cursorQuery.getInt(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_IS_SUMMARY)) == i) {
                                numValueOf = Integer.valueOf(cursorQuery.getInt(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID)));
                            } else {
                                String string2 = cursorQuery.getString(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_TITLE));
                                if (string2 == null) {
                                    str2 = "";
                                } else {
                                    str2 = string2 + " ";
                                }
                                SpannableString spannableString = new SpannableString(str2 + cursorQuery.getString(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_MESSAGE)));
                                if (str2.length() > 0) {
                                    spannableString.setSpan(new StyleSpan(1), 0, str2.length(), 0);
                                }
                                arrayList.add(spannableString);
                                if (string == null) {
                                    string = cursorQuery.getString(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_FULL_DATA));
                                }
                            }
                            if (!cursorQuery.moveToNext()) {
                                break;
                            } else {
                                i = 1;
                            }
                        }
                        if (z && string != null) {
                            try {
                                jSONObject = new JSONObject(string);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        arrayList = null;
                        numValueOf = null;
                    }
                    if (cursorQuery != null && !cursorQuery.isClosed()) {
                        cursorQuery.close();
                    }
                    if (numValueOf == null) {
                        numValueOf = Integer.valueOf(secureRandom.nextInt());
                        createSummaryIdDatabaseEntry(oneSignalDbHelper, strOptString, numValueOf.intValue());
                    }
                    PendingIntent newActionPendingIntent2 = getNewActionPendingIntent(secureRandom.nextInt(), createBaseSummaryIntent(numValueOf.intValue(), jSONObject, strOptString));
                    if (arrayList != null && ((z && arrayList.size() > 1) || (!z && arrayList.size() > 0))) {
                        int size = arrayList.size() + (!z ? 1 : 0);
                        String strOptString2 = jSONObject.optString("grp_msg", null);
                        if (strOptString2 == null) {
                            charSequenceReplace = size + " new messages";
                        } else {
                            charSequenceReplace = strOptString2.replace("$[notif_count]", "" + size);
                        }
                        NotificationCompat.Builder builder = getBaseOneSignalNotificationBuilder(notificationGenerationJob).compatBuilder;
                        if (z) {
                            removeNotifyOptions(builder);
                        } else {
                            if (notificationGenerationJob.overriddenSound != null) {
                                builder.setSound(notificationGenerationJob.overriddenSound);
                            }
                            if (notificationGenerationJob.overriddenFlags != null) {
                                builder.setDefaults(notificationGenerationJob.overriddenFlags.intValue());
                            }
                        }
                        builder.setContentIntent(newActionPendingIntent2).setDeleteIntent(newActionPendingIntent).setContentTitle(currentContext.getPackageManager().getApplicationLabel(currentContext.getApplicationInfo())).setContentText(charSequenceReplace).setNumber(size).setSmallIcon(getDefaultSmallIconId()).setLargeIcon(getDefaultLargeIcon()).setOnlyAlertOnce(z).setGroup(strOptString).setGroupSummary(true);
                        try {
                            builder.setGroupAlertBehavior(1);
                        } catch (Throwable unused) {
                        }
                        if (!z) {
                            builder.setTicker(charSequenceReplace);
                        }
                        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                        if (!z) {
                            String string3 = notificationGenerationJob.getTitle() != null ? notificationGenerationJob.getTitle().toString() : null;
                            if (string3 == null) {
                                str = "";
                            } else {
                                str = string3 + " ";
                            }
                            SpannableString spannableString2 = new SpannableString(str + notificationGenerationJob.getBody().toString());
                            if (str.length() > 0) {
                                spannableString2.setSpan(new StyleSpan(1), 0, str.length(), 0);
                            }
                            inboxStyle.addLine(spannableString2);
                        }
                        Iterator it = arrayList.iterator();
                        while (it.hasNext()) {
                            inboxStyle.addLine((SpannableString) it.next());
                        }
                        inboxStyle.setBigContentTitle(charSequenceReplace);
                        builder.setStyle(inboxStyle);
                        notificationBuild = builder.build();
                    } else {
                        NotificationCompat.Builder builder2 = oneSignalNotificationBuilder.compatBuilder;
                        builder2.mActions.clear();
                        addNotificationActionButtons(jSONObject, builder2, numValueOf.intValue(), strOptString);
                        builder2.setContentIntent(newActionPendingIntent2).setDeleteIntent(newActionPendingIntent).setOnlyAlertOnce(z).setGroup(strOptString).setGroupSummary(true);
                        try {
                            builder2.setGroupAlertBehavior(1);
                        } catch (Throwable unused2) {
                        }
                        notificationBuild = builder2.build();
                        addXiaomiSettings(oneSignalNotificationBuilder, notificationBuild);
                    }
                    NotificationManagerCompat.from(currentContext).notify(numValueOf.intValue(), notificationBuild);
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null && !cursorQuery.isClosed()) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                cursorQuery = null;
            }
        } catch (Throwable th3) {
            th = th3;
            cursorQuery = null;
        }
    }

    private static Intent createBaseSummaryIntent(int i, JSONObject jSONObject, String str) {
        return getNewBaseIntent(i).putExtra("onesignal_data", jSONObject.toString()).putExtra("summary", str);
    }

    private static void createSummaryIdDatabaseEntry(OneSignalDbHelper oneSignalDbHelper, String str, int i) throws Throwable {
        SQLiteDatabase writableDbWithRetries;
        SQLiteDatabase sQLiteDatabase = null;
        try {
            try {
                try {
                    writableDbWithRetries = oneSignalDbHelper.getWritableDbWithRetries();
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Throwable th2) {
                th = th2;
                writableDbWithRetries = sQLiteDatabase;
            }
            try {
                writableDbWithRetries.beginTransaction();
                ContentValues contentValues = new ContentValues();
                contentValues.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID, Integer.valueOf(i));
                contentValues.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_GROUP_ID, str);
                contentValues.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_IS_SUMMARY, (Integer) 1);
                writableDbWithRetries.insertOrThrow(OneSignalDbContract.NotificationTable.TABLE_NAME, null, contentValues);
                writableDbWithRetries.setTransactionSuccessful();
            } catch (Throwable th3) {
                th = th3;
                sQLiteDatabase = writableDbWithRetries;
                OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error adding summary notification record! ", th);
                if (sQLiteDatabase == null) {
                    return;
                } else {
                    sQLiteDatabase.endTransaction();
                }
            }
            if (writableDbWithRetries != null) {
                writableDbWithRetries.endTransaction();
            }
        } catch (Throwable th4) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error closing transaction! ", th4);
        }
    }

    private static void addBackgroundImage(JSONObject jSONObject, NotificationCompat.Builder builder) throws Throwable {
        Bitmap bitmapFromAssetsOrResourceName;
        JSONObject jSONObject2;
        String string;
        if (Build.VERSION.SDK_INT < 16) {
            return;
        }
        String strOptString = jSONObject.optString("bg_img", null);
        if (strOptString != null) {
            jSONObject2 = new JSONObject(strOptString);
            bitmapFromAssetsOrResourceName = getBitmap(jSONObject2.optString("img", null));
        } else {
            bitmapFromAssetsOrResourceName = null;
            jSONObject2 = null;
        }
        if (bitmapFromAssetsOrResourceName == null) {
            bitmapFromAssetsOrResourceName = getBitmapFromAssetsOrResourceName("onesignal_bgimage_default_image");
        }
        if (bitmapFromAssetsOrResourceName != null) {
            RemoteViews remoteViews = new RemoteViews(currentContext.getPackageName(), R.layout.onesignal_bgimage_notif_layout);
            remoteViews.setTextViewText(R.id.os_bgimage_notif_title, getTitle(jSONObject));
            remoteViews.setTextViewText(R.id.os_bgimage_notif_body, jSONObject.optString("alert"));
            setTextColor(remoteViews, jSONObject2, R.id.os_bgimage_notif_title, "tc", "onesignal_bgimage_notif_title_color");
            setTextColor(remoteViews, jSONObject2, R.id.os_bgimage_notif_body, "bc", "onesignal_bgimage_notif_body_color");
            if (jSONObject2 != null && jSONObject2.has("img_align")) {
                string = jSONObject2.getString("img_align");
            } else {
                int identifier = contextResources.getIdentifier("onesignal_bgimage_notif_image_align", "string", packageName);
                string = identifier != 0 ? contextResources.getString(identifier) : null;
            }
            if ("right".equals(string)) {
                remoteViews.setViewPadding(R.id.os_bgimage_notif_bgimage_align_layout, -5000, 0, 0, 0);
                remoteViews.setImageViewBitmap(R.id.os_bgimage_notif_bgimage_right_aligned, bitmapFromAssetsOrResourceName);
                remoteViews.setViewVisibility(R.id.os_bgimage_notif_bgimage_right_aligned, 0);
                remoteViews.setViewVisibility(R.id.os_bgimage_notif_bgimage, 2);
            } else {
                remoteViews.setImageViewBitmap(R.id.os_bgimage_notif_bgimage, bitmapFromAssetsOrResourceName);
            }
            builder.setContent(remoteViews);
            builder.setStyle(null);
        }
    }

    private static void setTextColor(RemoteViews remoteViews, JSONObject jSONObject, int i, String str, String str2) {
        Integer numSafeGetColorFromHex = safeGetColorFromHex(jSONObject, str);
        if (numSafeGetColorFromHex != null) {
            remoteViews.setTextColor(i, numSafeGetColorFromHex.intValue());
            return;
        }
        int identifier = contextResources.getIdentifier(str2, "color", packageName);
        if (identifier != 0) {
            remoteViews.setTextColor(i, AndroidSupportV4Compat.ContextCompat.getColor(currentContext, identifier));
        }
    }

    private static Integer safeGetColorFromHex(JSONObject jSONObject, String str) {
        if (jSONObject == null) {
            return null;
        }
        try {
            if (jSONObject.has(str)) {
                return Integer.valueOf(new BigInteger(jSONObject.optString(str), 16).intValue());
            }
            return null;
        } catch (Throwable unused) {
            return null;
        }
    }

    private static Bitmap getLargeIcon(JSONObject jSONObject) {
        Bitmap bitmap = getBitmap(jSONObject.optString("licon"));
        if (bitmap == null) {
            bitmap = getBitmapFromAssetsOrResourceName("ic_onesignal_large_icon_default");
        }
        if (bitmap == null) {
            return null;
        }
        return resizeBitmapForLargeIconArea(bitmap);
    }

    private static Bitmap getDefaultLargeIcon() {
        return resizeBitmapForLargeIconArea(getBitmapFromAssetsOrResourceName("ic_onesignal_large_icon_default"));
    }

    private static Bitmap resizeBitmapForLargeIconArea(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        try {
            int dimension = (int) contextResources.getDimension(android.R.dimen.notification_large_icon_height);
            int dimension2 = (int) contextResources.getDimension(android.R.dimen.notification_large_icon_width);
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();
            if (width > dimension2 || height > dimension) {
                if (height > width) {
                    dimension2 = (int) (dimension * (width / height));
                } else if (width > height) {
                    dimension = (int) (dimension2 * (height / width));
                }
                return Bitmap.createScaledBitmap(bitmap, dimension2, dimension, true);
            }
        } catch (Throwable unused) {
        }
        return bitmap;
    }

    private static Bitmap getBitmapFromAssetsOrResourceName(String str) {
        Bitmap bitmapDecodeStream;
        try {
            bitmapDecodeStream = BitmapFactory.decodeStream(currentContext.getAssets().open(str));
        } catch (Throwable unused) {
            bitmapDecodeStream = null;
        }
        if (bitmapDecodeStream != null) {
            return bitmapDecodeStream;
        }
        try {
            for (String str2 : Arrays.asList(".png", ".webp", ".jpg", ".gif", ".bmp")) {
                try {
                    bitmapDecodeStream = BitmapFactory.decodeStream(currentContext.getAssets().open(str + str2));
                } catch (Throwable unused2) {
                }
                if (bitmapDecodeStream != null) {
                    return bitmapDecodeStream;
                }
            }
            int resourceIcon = getResourceIcon(str);
            if (resourceIcon != 0) {
                return BitmapFactory.decodeResource(contextResources, resourceIcon);
            }
        } catch (Throwable unused3) {
        }
        return null;
    }

    private static Bitmap getBitmapFromURL(String str) {
        try {
            return BitmapFactory.decodeStream(new URL(str).openConnection().getInputStream());
        } catch (Throwable th) {
            OneSignal.Log(OneSignal.LOG_LEVEL.WARN, "Could not download image!", th);
            return null;
        }
    }

    private static Bitmap getBitmap(String str) {
        if (str == null) {
            return null;
        }
        String strTrim = str.trim();
        if (strTrim.startsWith("http://") || strTrim.startsWith("https://")) {
            return getBitmapFromURL(strTrim);
        }
        return getBitmapFromAssetsOrResourceName(str);
    }

    private static int getResourceIcon(String str) {
        if (str == null) {
            return 0;
        }
        String strTrim = str.trim();
        if (!OSUtils.isValidResourceName(strTrim)) {
            return 0;
        }
        int drawableId = getDrawableId(strTrim);
        if (drawableId != 0) {
            return drawableId;
        }
        try {
            return R.drawable.class.getField(str).getInt(null);
        } catch (Throwable unused) {
            return 0;
        }
    }

    private static int getSmallIconId(JSONObject jSONObject) {
        int resourceIcon = getResourceIcon(jSONObject.optString("sicon", null));
        return resourceIcon != 0 ? resourceIcon : getDefaultSmallIconId();
    }

    private static int getDefaultSmallIconId() {
        int drawableId = getDrawableId("ic_stat_onesignal_default");
        if (drawableId != 0) {
            return drawableId;
        }
        int drawableId2 = getDrawableId("corona_statusbar_icon_default");
        if (drawableId2 != 0) {
            return drawableId2;
        }
        int drawableId3 = getDrawableId("ic_os_notification_fallback_white_24dp");
        return drawableId3 != 0 ? drawableId3 : android.R.drawable.ic_popup_reminder;
    }

    private static int getDrawableId(String str) {
        return contextResources.getIdentifier(str, "drawable", packageName);
    }

    private static boolean isSoundEnabled(JSONObject jSONObject) {
        String strOptString = jSONObject.optString("sound", null);
        if ("null".equals(strOptString) || "nil".equals(strOptString)) {
            return false;
        }
        return OneSignal.getSoundEnabled(currentContext);
    }

    private static BigInteger getAccentColor(JSONObject jSONObject) {
        try {
            if (jSONObject.has("bgac")) {
                return new BigInteger(jSONObject.optString("bgac", null), 16);
            }
        } catch (Throwable unused) {
        }
        try {
            String manifestMeta = OSUtils.getManifestMeta(currentContext, "com.onesignal.NotificationAccentColor.DEFAULT");
            if (manifestMeta != null) {
                return new BigInteger(manifestMeta, 16);
            }
        } catch (Throwable unused2) {
        }
        return null;
    }

    private static void addNotificationActionButtons(JSONObject jSONObject, NotificationCompat.Builder builder, int i, String str) {
        try {
            JSONObject jSONObject2 = new JSONObject(jSONObject.optString("custom"));
            if (jSONObject2.has("a")) {
                JSONObject jSONObject3 = jSONObject2.getJSONObject("a");
                if (jSONObject3.has("actionButtons")) {
                    JSONArray jSONArray = jSONObject3.getJSONArray("actionButtons");
                    for (int i2 = 0; i2 < jSONArray.length(); i2++) {
                        JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i2);
                        JSONObject jSONObject4 = new JSONObject(jSONObject.toString());
                        Intent newBaseIntent = getNewBaseIntent(i);
                        newBaseIntent.setAction("" + i2);
                        newBaseIntent.putExtra("action_button", true);
                        jSONObject4.put("actionSelected", jSONObjectOptJSONObject.optString("id"));
                        newBaseIntent.putExtra("onesignal_data", jSONObject4.toString());
                        if (str != null) {
                            newBaseIntent.putExtra("summary", str);
                        } else if (jSONObject.has("grp")) {
                            newBaseIntent.putExtra("grp", jSONObject.optString("grp"));
                        }
                        builder.addAction(jSONObjectOptJSONObject.has("icon") ? getResourceIcon(jSONObjectOptJSONObject.optString("icon")) : 0, jSONObjectOptJSONObject.optString("text"), getNewActionPendingIntent(i, newBaseIntent));
                    }
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void addAlertButtons(Context context, JSONObject jSONObject, List<String> list, List<String> list2) {
        try {
            addCustomAlertButtons(context, jSONObject, list, list2);
        } catch (Throwable th) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Failed to parse JSON for custom buttons for alert dialog.", th);
        }
        if (list.size() == 0 || list.size() < 3) {
            list.add(OSUtils.getResourceString(context, "onesignal_in_app_alert_ok_button_text", "Ok"));
            list2.add("__DEFAULT__");
        }
    }

    private static void addCustomAlertButtons(Context context, JSONObject jSONObject, List<String> list, List<String> list2) throws JSONException {
        JSONObject jSONObject2 = new JSONObject(jSONObject.optString("custom"));
        if (jSONObject2.has("a")) {
            JSONObject jSONObject3 = jSONObject2.getJSONObject("a");
            if (jSONObject3.has("actionButtons")) {
                JSONArray jSONArrayOptJSONArray = jSONObject3.optJSONArray("actionButtons");
                for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                    JSONObject jSONObject4 = jSONArrayOptJSONArray.getJSONObject(i);
                    list.add(jSONObject4.optString("text"));
                    list2.add(jSONObject4.optString("id"));
                }
            }
        }
    }
}
