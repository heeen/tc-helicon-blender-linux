package com.onesignal;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.support.annotation.RequiresApi;

/* JADX INFO: loaded from: classes.dex */
@RequiresApi(api = 22)
public class GcmIntentJobService extends OneSignalJobServiceBase {
    @Override // com.onesignal.OneSignalJobServiceBase, android.app.job.JobService
    public /* bridge */ /* synthetic */ boolean onStartJob(JobParameters jobParameters) {
        return super.onStartJob(jobParameters);
    }

    @Override // com.onesignal.OneSignalJobServiceBase, android.app.job.JobService
    public /* bridge */ /* synthetic */ boolean onStopJob(JobParameters jobParameters) {
        return super.onStopJob(jobParameters);
    }

    @Override // com.onesignal.OneSignalJobServiceBase
    void startProcessing(JobService jobService, JobParameters jobParameters) throws Throwable {
        NotificationBundleProcessor.ProcessFromGCMIntentService(jobService, new BundleCompatPersistableBundle(jobParameters.getExtras()), null);
    }
}
