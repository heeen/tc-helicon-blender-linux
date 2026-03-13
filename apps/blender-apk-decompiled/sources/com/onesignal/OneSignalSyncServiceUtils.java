package com.onesignal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import com.onesignal.AndroidSupportV4Compat;
import com.onesignal.LocationGMS;
import com.onesignal.OneSignal;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: loaded from: classes.dex */
class OneSignalSyncServiceUtils {
    private static final int SYNC_AFTER_BG_DELAY_MS = 120000;
    private static final int SYNC_TASK_ID = 2071862118;
    private static Long nextScheduledSyncTime = 0L;
    private static AtomicBoolean runningOnFocusTime = new AtomicBoolean();
    private static Thread syncBgThread;

    OneSignalSyncServiceUtils() {
    }

    static void scheduleLocationUpdateTask(Context context, long j) {
        OneSignal.Log(OneSignal.LOG_LEVEL.VERBOSE, "scheduleLocationUpdateTask:delayMs: " + j);
        scheduleSyncTask(context, j);
    }

    static void scheduleSyncTask(Context context) {
        OneSignal.Log(OneSignal.LOG_LEVEL.VERBOSE, "scheduleSyncTask:SYNC_AFTER_BG_DELAY_MS: 120000");
        scheduleSyncTask(context, 120000L);
    }

    static void cancelSyncTask(Context context) {
        synchronized (nextScheduledSyncTime) {
            nextScheduledSyncTime = 0L;
            if (LocationGMS.scheduleUpdate(context)) {
                return;
            }
            if (useJob()) {
                ((JobScheduler) context.getSystemService("jobscheduler")).cancel(SYNC_TASK_ID);
            } else {
                ((AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM)).cancel(syncServicePendingIntent(context));
            }
        }
    }

    private static PendingIntent syncServicePendingIntent(Context context) {
        return PendingIntent.getService(context, SYNC_TASK_ID, new Intent(context, (Class<?>) SyncService.class), 134217728);
    }

    private static boolean useJob() {
        return Build.VERSION.SDK_INT >= 21;
    }

    private static void scheduleSyncTask(Context context, long j) {
        synchronized (nextScheduledSyncTime) {
            if (nextScheduledSyncTime.longValue() == 0 || System.currentTimeMillis() + j <= nextScheduledSyncTime.longValue()) {
                if (j < 5000) {
                    j = 5000;
                }
                if (useJob()) {
                    scheduleSyncServiceAsJob(context, j);
                } else {
                    scheduleSyncServiceAsAlarm(context, j);
                }
                nextScheduledSyncTime = Long.valueOf(System.currentTimeMillis() + j);
            }
        }
    }

    private static boolean hasBootPermission(Context context) {
        return AndroidSupportV4Compat.ContextCompat.checkSelfPermission(context, "android.permission.RECEIVE_BOOT_COMPLETED") == 0;
    }

    @RequiresApi(21)
    private static void scheduleSyncServiceAsJob(Context context, long j) {
        OneSignal.Log(OneSignal.LOG_LEVEL.VERBOSE, "scheduleSyncServiceAsJob:atTime: " + j);
        JobInfo.Builder builder = new JobInfo.Builder(SYNC_TASK_ID, new ComponentName(context, (Class<?>) SyncJobService.class));
        builder.setMinimumLatency(j).setRequiredNetworkType(1);
        if (hasBootPermission(context)) {
            builder.setPersisted(true);
        }
        try {
            int iSchedule = ((JobScheduler) context.getSystemService("jobscheduler")).schedule(builder.build());
            OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "scheduleSyncServiceAsJob:result: " + iSchedule);
        } catch (NullPointerException e) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "scheduleSyncServiceAsJob called JobScheduler.jobScheduler which triggered an internal null Android error. Skipping job.", e);
        }
    }

    private static void scheduleSyncServiceAsAlarm(Context context, long j) {
        OneSignal.Log(OneSignal.LOG_LEVEL.VERBOSE, "scheduleServiceSyncTask:atTime: " + j);
        ((AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM)).set(0, System.currentTimeMillis() + j + j, syncServicePendingIntent(context));
    }

    static void syncOnFocusTime() {
        if (runningOnFocusTime.get()) {
            return;
        }
        synchronized (runningOnFocusTime) {
            runningOnFocusTime.set(true);
            internalSyncOnFocusTime();
            runningOnFocusTime.set(false);
        }
    }

    private static void internalSyncOnFocusTime() {
        long jGetUnsentActiveTime = OneSignal.GetUnsentActiveTime();
        if (jGetUnsentActiveTime < 60) {
            return;
        }
        OneSignal.sendOnFocus(jGetUnsentActiveTime, true);
    }

    static void doBackgroundSync(Context context, SyncRunnable syncRunnable) {
        OneSignal.setAppContext(context);
        syncBgThread = new Thread(syncRunnable, "OS_SYNCSRV_BG_SYNC");
        syncBgThread.start();
    }

    static boolean stopSyncBgThread() {
        if (syncBgThread == null || !syncBgThread.isAlive()) {
            return false;
        }
        syncBgThread.interrupt();
        return true;
    }

    static abstract class SyncRunnable implements Runnable {
        protected abstract void stopSync();

        SyncRunnable() {
        }

        @Override // java.lang.Runnable
        public final void run() {
            synchronized (OneSignalSyncServiceUtils.nextScheduledSyncTime) {
                Long unused = OneSignalSyncServiceUtils.nextScheduledSyncTime = 0L;
            }
            if (OneSignal.getUserId() == null) {
                stopSync();
                return;
            }
            OneSignal.appId = OneSignal.getSavedAppId();
            OneSignalStateSynchronizer.initUserState();
            LocationGMS.getLocation(OneSignal.appContext, false, new LocationGMS.LocationHandler() { // from class: com.onesignal.OneSignalSyncServiceUtils.SyncRunnable.1
                @Override // com.onesignal.LocationGMS.LocationHandler
                public LocationGMS.CALLBACK_TYPE getType() {
                    return LocationGMS.CALLBACK_TYPE.SYNC_SERVICE;
                }

                @Override // com.onesignal.LocationGMS.LocationHandler
                public void complete(LocationGMS.LocationPoint locationPoint) {
                    if (locationPoint != null) {
                        OneSignalStateSynchronizer.updateLocation(locationPoint);
                    }
                    OneSignalStateSynchronizer.syncUserState(true);
                    OneSignalSyncServiceUtils.syncOnFocusTime();
                    SyncRunnable.this.stopSync();
                }
            });
        }
    }

    @RequiresApi(api = 21)
    static class LollipopSyncRunnable extends SyncRunnable {
        private JobParameters jobParameters;
        private JobService jobService;

        LollipopSyncRunnable(JobService jobService, JobParameters jobParameters) {
            this.jobService = jobService;
            this.jobParameters = jobParameters;
        }

        @Override // com.onesignal.OneSignalSyncServiceUtils.SyncRunnable
        protected void stopSync() {
            OneSignal.Log(OneSignal.LOG_LEVEL.DEBUG, "LollipopSyncRunnable:JobFinished");
            this.jobService.jobFinished(this.jobParameters, false);
        }
    }

    static class LegacySyncRunnable extends SyncRunnable {
        Service callerService;

        LegacySyncRunnable(Service service) {
            this.callerService = service;
        }

        @Override // com.onesignal.OneSignalSyncServiceUtils.SyncRunnable
        protected void stopSync() {
            OneSignal.Log(OneSignal.LOG_LEVEL.DEBUG, "LegacySyncRunnable:Stopped");
            this.callerService.stopSelf();
        }
    }
}
