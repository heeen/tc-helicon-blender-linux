package com.onesignal;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.support.annotation.RequiresApi;
import com.onesignal.OneSignalSyncServiceUtils;

/* JADX INFO: loaded from: classes.dex */
@RequiresApi(api = 21)
public class SyncJobService extends JobService {
    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters jobParameters) {
        OneSignalSyncServiceUtils.doBackgroundSync(this, new OneSignalSyncServiceUtils.LollipopSyncRunnable(this, jobParameters));
        return true;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters jobParameters) {
        return OneSignalSyncServiceUtils.stopSyncBgThread();
    }
}
