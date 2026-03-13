package com.google.android.gms.common;

import android.R;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.GuardedBy;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ProgressBar;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiActivity;
import com.google.android.gms.common.api.internal.zzbm;
import com.google.android.gms.common.api.internal.zzbx;
import com.google.android.gms.common.api.internal.zzby;
import com.google.android.gms.common.api.internal.zzcf;
import com.google.android.gms.common.api.internal.zzcn;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.common.internal.zzu;
import com.google.android.gms.common.internal.zzv;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.onesignal.OneSignalDbContract;
import java.util.ArrayList;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public class GoogleApiAvailability extends zzf {
    public static final String GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms";

    @GuardedBy("mLock")
    private String zzfqx;
    private static final Object mLock = new Object();
    private static final GoogleApiAvailability zzfqw = new GoogleApiAvailability();
    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = zzf.GOOGLE_PLAY_SERVICES_VERSION_CODE;

    @SuppressLint({"HandlerLeak"})
    class zza extends Handler {
        private final Context mApplicationContext;

        public zza(Context context) {
            super(Looper.myLooper() == null ? Looper.getMainLooper() : Looper.myLooper());
            this.mApplicationContext = context.getApplicationContext();
        }

        @Override // android.os.Handler
        public final void handleMessage(Message message) {
            if (message.what == 1) {
                int iIsGooglePlayServicesAvailable = GoogleApiAvailability.this.isGooglePlayServicesAvailable(this.mApplicationContext);
                if (GoogleApiAvailability.this.isUserResolvableError(iIsGooglePlayServicesAvailable)) {
                    GoogleApiAvailability.this.showErrorNotification(this.mApplicationContext, iIsGooglePlayServicesAvailable);
                    return;
                }
                return;
            }
            int i = message.what;
            StringBuilder sb = new StringBuilder(50);
            sb.append("Don't know how to handle this message: ");
            sb.append(i);
            Log.w("GoogleApiAvailability", sb.toString());
        }
    }

    GoogleApiAvailability() {
    }

    public static GoogleApiAvailability getInstance() {
        return zzfqw;
    }

    @Hide
    public static Dialog zza(Activity activity, DialogInterface.OnCancelListener onCancelListener) {
        ProgressBar progressBar = new ProgressBar(activity, null, R.attr.progressBarStyleLarge);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(0);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(progressBar);
        builder.setMessage(zzu.zzh(activity, 18));
        builder.setPositiveButton("", (DialogInterface.OnClickListener) null);
        AlertDialog alertDialogCreate = builder.create();
        zza(activity, alertDialogCreate, "GooglePlayServicesUpdatingDialog", onCancelListener);
        return alertDialogCreate;
    }

    static Dialog zza(Context context, int i, zzv zzvVar, DialogInterface.OnCancelListener onCancelListener) {
        if (i == 0) {
            return null;
        }
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.alertDialogTheme, typedValue, true);
        AlertDialog.Builder builder = "Theme.Dialog.Alert".equals(context.getResources().getResourceEntryName(typedValue.resourceId)) ? new AlertDialog.Builder(context, 5) : null;
        if (builder == null) {
            builder = new AlertDialog.Builder(context);
        }
        builder.setMessage(zzu.zzh(context, i));
        if (onCancelListener != null) {
            builder.setOnCancelListener(onCancelListener);
        }
        String strZzj = zzu.zzj(context, i);
        if (strZzj != null) {
            builder.setPositiveButton(strZzj, zzvVar);
        }
        String strZzf = zzu.zzf(context, i);
        if (strZzf != null) {
            builder.setTitle(strZzf);
        }
        return builder.create();
    }

    @Hide
    @Nullable
    public static zzbx zza(Context context, zzby zzbyVar) {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        intentFilter.addDataScheme("package");
        zzbx zzbxVar = new zzbx(zzbyVar);
        context.registerReceiver(zzbxVar, intentFilter);
        zzbxVar.setContext(context);
        if (zzs.zzr(context, "com.google.android.gms")) {
            return zzbxVar;
        }
        zzbyVar.zzaio();
        zzbxVar.unregister();
        return null;
    }

    @TargetApi(26)
    private final String zza(Context context, NotificationManager notificationManager) {
        zzbq.checkState(com.google.android.gms.common.util.zzs.isAtLeastO());
        String strZzahe = zzahe();
        if (strZzahe == null) {
            strZzahe = "com.google.android.gms.availability";
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel("com.google.android.gms.availability");
            String strZzco = zzu.zzco(context);
            if (notificationChannel == null) {
                notificationChannel = new NotificationChannel("com.google.android.gms.availability", strZzco, 4);
            } else if (!strZzco.equals(notificationChannel.getName())) {
                notificationChannel.setName(strZzco);
            }
            notificationManager.createNotificationChannel(notificationChannel);
        }
        return strZzahe;
    }

    static void zza(Activity activity, Dialog dialog, String str, DialogInterface.OnCancelListener onCancelListener) {
        if (activity instanceof FragmentActivity) {
            SupportErrorDialogFragment.newInstance(dialog, onCancelListener).show(((FragmentActivity) activity).getSupportFragmentManager(), str);
        } else {
            ErrorDialogFragment.newInstance(dialog, onCancelListener).show(activity.getFragmentManager(), str);
        }
    }

    @TargetApi(20)
    private final void zza(Context context, int i, String str, PendingIntent pendingIntent) {
        Notification notificationBuild;
        int i2;
        if (i == 18) {
            zzcd(context);
            return;
        }
        if (pendingIntent == null) {
            if (i == 6) {
                Log.w("GoogleApiAvailability", "Missing resolution for ConnectionResult.RESOLUTION_REQUIRED. Call GoogleApiAvailability#showErrorNotification(Context, ConnectionResult) instead.");
                return;
            }
            return;
        }
        String strZzg = zzu.zzg(context, i);
        String strZzi = zzu.zzi(context, i);
        Resources resources = context.getResources();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(OneSignalDbContract.NotificationTable.TABLE_NAME);
        if (com.google.android.gms.common.util.zzj.zzcv(context)) {
            zzbq.checkState(com.google.android.gms.common.util.zzs.zzanw());
            Notification.Builder builderAddAction = new Notification.Builder(context).setSmallIcon(context.getApplicationInfo().icon).setPriority(2).setAutoCancel(true).setContentTitle(strZzg).setStyle(new Notification.BigTextStyle().bigText(strZzi)).addAction(com.google.android.gms.R.drawable.common_full_open_on_phone, resources.getString(com.google.android.gms.R.string.common_open_on_phone), pendingIntent);
            if (com.google.android.gms.common.util.zzs.isAtLeastO() && com.google.android.gms.common.util.zzs.isAtLeastO()) {
                builderAddAction.setChannelId(zza(context, notificationManager));
            }
            notificationBuild = builderAddAction.build();
        } else {
            NotificationCompat.Builder style = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.stat_sys_warning).setTicker(resources.getString(com.google.android.gms.R.string.common_google_play_services_notification_ticker)).setWhen(System.currentTimeMillis()).setAutoCancel(true).setContentIntent(pendingIntent).setContentTitle(strZzg).setContentText(strZzi).setLocalOnly(true).setStyle(new NotificationCompat.BigTextStyle().bigText(strZzi));
            if (com.google.android.gms.common.util.zzs.isAtLeastO() && com.google.android.gms.common.util.zzs.isAtLeastO()) {
                style.setChannelId(zza(context, notificationManager));
            }
            notificationBuild = style.build();
        }
        switch (i) {
            case 1:
            case 2:
            case 3:
                i2 = 10436;
                zzs.zzfrv.set(false);
                break;
            default:
                i2 = 39789;
                break;
        }
        notificationManager.notify(i2, notificationBuild);
    }

    @VisibleForTesting(otherwise = 2)
    private final String zzahe() {
        String str;
        synchronized (mLock) {
            str = this.zzfqx;
        }
        return str;
    }

    public Task<Void> checkApiAvailability(GoogleApi<?> googleApi, GoogleApi<?>... googleApiArr) {
        zzbq.checkNotNull(googleApi, "Requested API must not be null.");
        for (GoogleApi<?> googleApi2 : googleApiArr) {
            zzbq.checkNotNull(googleApi2, "Requested API must not be null.");
        }
        ArrayList arrayList = new ArrayList(googleApiArr.length + 1);
        arrayList.add(googleApi);
        arrayList.addAll(Arrays.asList(googleApiArr));
        return zzbm.zzajy().zza(arrayList).continueWith(new zze(this));
    }

    public Dialog getErrorDialog(Activity activity, int i, int i2) {
        return getErrorDialog(activity, i, i2, null);
    }

    public Dialog getErrorDialog(Activity activity, int i, int i2, DialogInterface.OnCancelListener onCancelListener) {
        return zza(activity, i, zzv.zza(activity, zzf.zza(activity, i, "d"), i2), onCancelListener);
    }

    @Override // com.google.android.gms.common.zzf
    @Nullable
    public PendingIntent getErrorResolutionPendingIntent(Context context, int i, int i2) {
        return super.getErrorResolutionPendingIntent(context, i, i2);
    }

    @Nullable
    public PendingIntent getErrorResolutionPendingIntent(Context context, ConnectionResult connectionResult) {
        return connectionResult.hasResolution() ? connectionResult.getResolution() : getErrorResolutionPendingIntent(context, connectionResult.getErrorCode(), 0);
    }

    @Override // com.google.android.gms.common.zzf
    public final String getErrorString(int i) {
        return super.getErrorString(i);
    }

    @Override // com.google.android.gms.common.zzf
    public int isGooglePlayServicesAvailable(Context context) {
        return super.isGooglePlayServicesAvailable(context);
    }

    @Override // com.google.android.gms.common.zzf
    public final boolean isUserResolvableError(int i) {
        return super.isUserResolvableError(i);
    }

    @MainThread
    public Task<Void> makeGooglePlayServicesAvailable(Activity activity) {
        zzbq.zzgn("makeGooglePlayServicesAvailable must be called from the main thread");
        int iIsGooglePlayServicesAvailable = isGooglePlayServicesAvailable(activity);
        if (iIsGooglePlayServicesAvailable == 0) {
            return Tasks.forResult(null);
        }
        zzcn zzcnVarZzq = zzcn.zzq(activity);
        zzcnVarZzq.zzb(new ConnectionResult(iIsGooglePlayServicesAvailable, null), 0);
        return zzcnVarZzq.getTask();
    }

    @TargetApi(26)
    public void setDefaultNotificationChannelId(@NonNull Context context, @NonNull String str) {
        if (com.google.android.gms.common.util.zzs.isAtLeastO()) {
            zzbq.checkNotNull(((NotificationManager) context.getSystemService(OneSignalDbContract.NotificationTable.TABLE_NAME)).getNotificationChannel(str));
        }
        synchronized (mLock) {
            this.zzfqx = str;
        }
    }

    public boolean showErrorDialogFragment(Activity activity, int i, int i2) {
        return showErrorDialogFragment(activity, i, i2, null);
    }

    public boolean showErrorDialogFragment(Activity activity, int i, int i2, DialogInterface.OnCancelListener onCancelListener) {
        Dialog errorDialog = getErrorDialog(activity, i, i2, onCancelListener);
        if (errorDialog == null) {
            return false;
        }
        zza(activity, errorDialog, GooglePlayServicesUtil.GMS_ERROR_DIALOG, onCancelListener);
        return true;
    }

    public void showErrorNotification(Context context, int i) {
        zza(context, i, (String) null, zza(context, i, 0, "n"));
    }

    public void showErrorNotification(Context context, ConnectionResult connectionResult) {
        zza(context, connectionResult.getErrorCode(), (String) null, getErrorResolutionPendingIntent(context, connectionResult));
    }

    @Hide
    public final boolean zza(Activity activity, @NonNull zzcf zzcfVar, int i, int i2, DialogInterface.OnCancelListener onCancelListener) {
        Dialog dialogZza = zza(activity, i, zzv.zza(zzcfVar, zzf.zza(activity, i, "d"), 2), onCancelListener);
        if (dialogZza == null) {
            return false;
        }
        zza(activity, dialogZza, GooglePlayServicesUtil.GMS_ERROR_DIALOG, onCancelListener);
        return true;
    }

    @Hide
    public final boolean zza(Context context, ConnectionResult connectionResult, int i) {
        PendingIntent errorResolutionPendingIntent = getErrorResolutionPendingIntent(context, connectionResult);
        if (errorResolutionPendingIntent == null) {
            return false;
        }
        zza(context, connectionResult.getErrorCode(), (String) null, GoogleApiActivity.zza(context, errorResolutionPendingIntent, i));
        return true;
    }

    final void zzcd(Context context) {
        new zza(context).sendEmptyMessageDelayed(1, 120000L);
    }
}
