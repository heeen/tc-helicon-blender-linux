package com.onesignal;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import com.onesignal.NotificationBundleProcessor;
import java.security.SecureRandom;

/* JADX INFO: loaded from: classes.dex */
public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
    private static final String GCM_RECEIVE_ACTION = "com.google.android.c2dm.intent.RECEIVE";
    private static final String GCM_TYPE = "gcm";
    private static final String MESSAGE_TYPE_EXTRA_KEY = "message_type";

    private static boolean isGcmMessage(Intent intent) {
        if (!GCM_RECEIVE_ACTION.equals(intent.getAction())) {
            return false;
        }
        String stringExtra = intent.getStringExtra(MESSAGE_TYPE_EXTRA_KEY);
        return stringExtra == null || GCM_TYPE.equals(stringExtra);
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) throws Throwable {
        Bundle extras = intent.getExtras();
        if (extras == null || "google.com/iid".equals(extras.getString("from"))) {
            return;
        }
        OneSignal.setAppContext(context);
        NotificationBundleProcessor.ProcessedBundleResult processedBundleResultProcessOrderBroadcast = processOrderBroadcast(context, intent, extras);
        if (processedBundleResultProcessOrderBroadcast == null) {
            setSuccessfulResultCode();
            return;
        }
        if (processedBundleResultProcessOrderBroadcast.isDup || processedBundleResultProcessOrderBroadcast.hasExtenderService) {
            setAbort();
        } else if (processedBundleResultProcessOrderBroadcast.isOneSignalPayload && OneSignal.getFilterOtherGCMReceivers(context)) {
            setAbort();
        } else {
            setSuccessfulResultCode();
        }
    }

    private void setSuccessfulResultCode() {
        if (isOrderedBroadcast()) {
            setResultCode(-1);
        }
    }

    private void setAbort() {
        if (isOrderedBroadcast()) {
            abortBroadcast();
            setResultCode(-1);
        }
    }

    private static NotificationBundleProcessor.ProcessedBundleResult processOrderBroadcast(Context context, Intent intent, Bundle bundle) throws Throwable {
        if (!isGcmMessage(intent)) {
            return null;
        }
        NotificationBundleProcessor.ProcessedBundleResult processedBundleResultProcessBundleFromReceiver = NotificationBundleProcessor.processBundleFromReceiver(context, bundle);
        if (processedBundleResultProcessBundleFromReceiver.processed()) {
            return processedBundleResultProcessBundleFromReceiver;
        }
        startGCMService(context, bundle);
        return processedBundleResultProcessBundleFromReceiver;
    }

    private static void startGCMService(Context context, Bundle bundle) throws Throwable {
        if (!NotificationBundleProcessor.hasRemoteResource(bundle)) {
            NotificationBundleProcessor.ProcessFromGCMIntentService(context, setCompatBundleForServer(bundle, BundleCompatFactory.getInstance()), null);
            return;
        }
        if (!(Integer.parseInt(bundle.getString("pri", "0")) > 9) && Build.VERSION.SDK_INT >= 26) {
            startGCMServiceWithJobScheduler(context, bundle);
            return;
        }
        try {
            startGCMServiceWithWakefulService(context, bundle);
        } catch (IllegalStateException e) {
            if (Build.VERSION.SDK_INT >= 21) {
                startGCMServiceWithJobScheduler(context, bundle);
                return;
            }
            throw e;
        }
    }

    @TargetApi(21)
    private static void startGCMServiceWithJobScheduler(Context context, Bundle bundle) {
        BundleCompat compatBundleForServer = setCompatBundleForServer(bundle, BundleCompatFactory.getInstance());
        ((JobScheduler) context.getSystemService("jobscheduler")).schedule(new JobInfo.Builder(new SecureRandom().nextInt(), new ComponentName(context.getPackageName(), GcmIntentJobService.class.getName())).setOverrideDeadline(0L).setExtras((PersistableBundle) compatBundleForServer.getBundle()).build());
    }

    private static void startGCMServiceWithWakefulService(Context context, Bundle bundle) {
        startWakefulService(context, new Intent().replaceExtras((Bundle) setCompatBundleForServer(bundle, new BundleCompatBundle()).getBundle()).setComponent(new ComponentName(context.getPackageName(), GcmIntentService.class.getName())));
    }

    private static BundleCompat setCompatBundleForServer(Bundle bundle, BundleCompat bundleCompat) {
        bundleCompat.putString("json_payload", NotificationBundleProcessor.bundleAsJSONObject(bundle).toString());
        bundleCompat.putLong("timestamp", Long.valueOf(System.currentTimeMillis() / 1000));
        return bundleCompat;
    }
}
