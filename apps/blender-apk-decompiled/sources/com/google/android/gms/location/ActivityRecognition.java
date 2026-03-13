package com.google.android.gms.location;

import android.app.Activity;
import android.content.Context;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzcfk;
import com.google.android.gms.internal.zzchh;

/* JADX INFO: loaded from: classes.dex */
public class ActivityRecognition {
    public static final String CLIENT_NAME = "activity_recognition";
    private static final Api.zzf<zzchh> zzegu = new Api.zzf<>();
    private static final Api.zza<zzchh, Api.ApiOptions.NoOptions> zzegv = new com.google.android.gms.location.zza();
    public static final Api<Api.ApiOptions.NoOptions> API = new Api<>("ActivityRecognition.API", zzegv, zzegu);

    @Deprecated
    public static final ActivityRecognitionApi ActivityRecognitionApi = new zzcfk();

    @Hide
    public static abstract class zza<R extends Result> extends com.google.android.gms.common.api.internal.zzm<R, zzchh> {
        public zza(GoogleApiClient googleApiClient) {
            super(ActivityRecognition.API, googleApiClient);
        }

        @Override // com.google.android.gms.common.api.internal.zzm, com.google.android.gms.common.api.internal.zzn
        @Hide
        public final /* bridge */ /* synthetic */ void setResult(Object obj) {
            super.setResult((Result) obj);
        }
    }

    private ActivityRecognition() {
    }

    public static ActivityRecognitionClient getClient(Activity activity) {
        return new ActivityRecognitionClient(activity);
    }

    public static ActivityRecognitionClient getClient(Context context) {
        return new ActivityRecognitionClient(context);
    }
}
