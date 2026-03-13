package com.kobakei.ratethisapp;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public class RateThisApp {
    public static final boolean DEBUG = false;
    private static final String KEY_ASK_LATER_DATE = "rta_ask_later_date";
    private static final String KEY_INSTALL_DATE = "rta_install_date";
    private static final String KEY_LAUNCH_TIMES = "rta_launch_times";
    private static final String KEY_OPT_OUT = "rta_opt_out";
    private static final String PREF_NAME = "RateThisApp";
    private static final String TAG = "RateThisApp";
    private static Date mInstallDate = new Date();
    private static int mLaunchTimes = 0;
    private static boolean mOptOut = false;
    private static Date mAskLaterDate = new Date();
    private static Config sConfig = new Config();
    private static Callback sCallback = null;
    private static WeakReference<AlertDialog> sDialogRef = null;

    public interface Callback {
        void onCancelClicked();

        void onNoClicked();

        void onYesClicked();
    }

    private static void log(String str) {
    }

    public static void init(Config config) {
        sConfig = config;
    }

    public static void setCallback(Callback callback) {
        sCallback = callback;
    }

    public static void onCreate(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("RateThisApp", 0);
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        if (sharedPreferences.getLong(KEY_INSTALL_DATE, 0L) == 0) {
            storeInstallDate(context, editorEdit);
        }
        int i = sharedPreferences.getInt(KEY_LAUNCH_TIMES, 0) + 1;
        editorEdit.putInt(KEY_LAUNCH_TIMES, i);
        log("Launch times; " + i);
        editorEdit.apply();
        mInstallDate = new Date(sharedPreferences.getLong(KEY_INSTALL_DATE, 0L));
        mLaunchTimes = sharedPreferences.getInt(KEY_LAUNCH_TIMES, 0);
        mOptOut = sharedPreferences.getBoolean(KEY_OPT_OUT, false);
        mAskLaterDate = new Date(sharedPreferences.getLong(KEY_ASK_LATER_DATE, 0L));
        printStatus(context);
    }

    @Deprecated
    public static void onStart(Context context) {
        onCreate(context);
    }

    public static boolean showRateDialogIfNeeded(Context context) {
        if (!shouldShowRateDialog()) {
            return false;
        }
        showRateDialog(context);
        return true;
    }

    public static boolean showRateDialogIfNeeded(Context context, int i) {
        if (!shouldShowRateDialog()) {
            return false;
        }
        showRateDialog(context, i);
        return true;
    }

    public static boolean shouldShowRateDialog() {
        if (mOptOut) {
            return false;
        }
        if (mLaunchTimes >= sConfig.mCriteriaLaunchTimes) {
            return true;
        }
        long millis = TimeUnit.DAYS.toMillis(sConfig.mCriteriaInstallDays);
        return new Date().getTime() - mInstallDate.getTime() >= millis && new Date().getTime() - mAskLaterDate.getTime() >= millis;
    }

    public static void showRateDialog(Context context) {
        showRateDialog(context, new AlertDialog.Builder(context));
    }

    public static void showRateDialog(Context context, int i) {
        showRateDialog(context, new AlertDialog.Builder(context, i));
    }

    public static void stopRateDialog(Context context) {
        setOptOut(context, true);
    }

    public static int getLaunchCount(Context context) {
        return context.getSharedPreferences("RateThisApp", 0).getInt(KEY_LAUNCH_TIMES, 0);
    }

    private static void showRateDialog(final Context context, AlertDialog.Builder builder) {
        if (sDialogRef == null || sDialogRef.get() == null) {
            int i = sConfig.mTitleId != 0 ? sConfig.mTitleId : R.string.rta_dialog_title;
            int i2 = sConfig.mMessageId != 0 ? sConfig.mMessageId : R.string.rta_dialog_message;
            int i3 = sConfig.mCancelButton != 0 ? sConfig.mCancelButton : R.string.rta_dialog_cancel;
            int i4 = sConfig.mNoButtonId != 0 ? sConfig.mNoButtonId : R.string.rta_dialog_no;
            int i5 = sConfig.mYesButtonId != 0 ? sConfig.mYesButtonId : R.string.rta_dialog_ok;
            builder.setTitle(i);
            builder.setMessage(i2);
            builder.setCancelable(sConfig.mCancelable);
            builder.setPositiveButton(i5, new DialogInterface.OnClickListener() { // from class: com.kobakei.ratethisapp.RateThisApp.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i6) {
                    if (RateThisApp.sCallback != null) {
                        RateThisApp.sCallback.onYesClicked();
                    }
                    String str = "market://details?id=" + context.getPackageName();
                    if (!TextUtils.isEmpty(RateThisApp.sConfig.mUrl)) {
                        str = RateThisApp.sConfig.mUrl;
                    }
                    try {
                        context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(str)));
                    } catch (ActivityNotFoundException unused) {
                        context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
                    }
                    RateThisApp.setOptOut(context, true);
                }
            });
            builder.setNeutralButton(i3, new DialogInterface.OnClickListener() { // from class: com.kobakei.ratethisapp.RateThisApp.2
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i6) {
                    if (RateThisApp.sCallback != null) {
                        RateThisApp.sCallback.onCancelClicked();
                    }
                    RateThisApp.clearSharedPreferences(context);
                    RateThisApp.storeAskLaterDate(context);
                }
            });
            builder.setNegativeButton(i4, new DialogInterface.OnClickListener() { // from class: com.kobakei.ratethisapp.RateThisApp.3
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i6) {
                    if (RateThisApp.sCallback != null) {
                        RateThisApp.sCallback.onNoClicked();
                    }
                    RateThisApp.setOptOut(context, true);
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.kobakei.ratethisapp.RateThisApp.4
                @Override // android.content.DialogInterface.OnCancelListener
                public void onCancel(DialogInterface dialogInterface) {
                    if (RateThisApp.sCallback != null) {
                        RateThisApp.sCallback.onCancelClicked();
                    }
                    RateThisApp.clearSharedPreferences(context);
                    RateThisApp.storeAskLaterDate(context);
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.kobakei.ratethisapp.RateThisApp.5
                @Override // android.content.DialogInterface.OnDismissListener
                public void onDismiss(DialogInterface dialogInterface) {
                    RateThisApp.sDialogRef.clear();
                }
            });
            sDialogRef = new WeakReference<>(builder.show());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void clearSharedPreferences(Context context) {
        SharedPreferences.Editor editorEdit = context.getSharedPreferences("RateThisApp", 0).edit();
        editorEdit.remove(KEY_INSTALL_DATE);
        editorEdit.remove(KEY_LAUNCH_TIMES);
        editorEdit.apply();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void setOptOut(Context context, boolean z) {
        SharedPreferences.Editor editorEdit = context.getSharedPreferences("RateThisApp", 0).edit();
        editorEdit.putBoolean(KEY_OPT_OUT, z);
        editorEdit.apply();
        mOptOut = z;
    }

    private static void storeInstallDate(Context context, SharedPreferences.Editor editor) {
        Date date = new Date();
        if (Build.VERSION.SDK_INT >= 9) {
            try {
                date = new Date(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).firstInstallTime);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        editor.putLong(KEY_INSTALL_DATE, date.getTime());
        log("First install: " + date.toString());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void storeAskLaterDate(Context context) {
        SharedPreferences.Editor editorEdit = context.getSharedPreferences("RateThisApp", 0).edit();
        editorEdit.putLong(KEY_ASK_LATER_DATE, System.currentTimeMillis());
        editorEdit.apply();
    }

    private static void printStatus(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("RateThisApp", 0);
        log("*** RateThisApp Status ***");
        log("Install Date: " + new Date(sharedPreferences.getLong(KEY_INSTALL_DATE, 0L)));
        log("Launch Times: " + sharedPreferences.getInt(KEY_LAUNCH_TIMES, 0));
        log("Opt out: " + sharedPreferences.getBoolean(KEY_OPT_OUT, false));
    }

    public static class Config {
        private int mCancelButton;
        private boolean mCancelable;
        private int mCriteriaInstallDays;
        private int mCriteriaLaunchTimes;
        private int mMessageId;
        private int mNoButtonId;
        private int mTitleId;
        private String mUrl;
        private int mYesButtonId;

        public Config() {
            this(7, 10);
        }

        public Config(int i, int i2) {
            this.mUrl = null;
            this.mTitleId = 0;
            this.mMessageId = 0;
            this.mYesButtonId = 0;
            this.mNoButtonId = 0;
            this.mCancelButton = 0;
            this.mCancelable = true;
            this.mCriteriaInstallDays = i;
            this.mCriteriaLaunchTimes = i2;
        }

        public void setTitle(@StringRes int i) {
            this.mTitleId = i;
        }

        public void setMessage(@StringRes int i) {
            this.mMessageId = i;
        }

        public void setYesButtonText(@StringRes int i) {
            this.mYesButtonId = i;
        }

        public void setNoButtonText(@StringRes int i) {
            this.mNoButtonId = i;
        }

        public void setCancelButtonText(@StringRes int i) {
            this.mCancelButton = i;
        }

        public void setUrl(String str) {
            this.mUrl = str;
        }

        public void setCancelable(boolean z) {
            this.mCancelable = z;
        }
    }
}
