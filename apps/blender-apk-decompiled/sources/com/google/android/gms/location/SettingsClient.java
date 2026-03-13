package com.google.android.gms.location;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.internal.zzda;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbj;
import com.google.android.gms.tasks.Task;

/* JADX INFO: loaded from: classes.dex */
public class SettingsClient extends GoogleApi<Api.ApiOptions.NoOptions> {
    @Hide
    public SettingsClient(@NonNull Activity activity) {
        super(activity, (Api<Api.ApiOptions>) LocationServices.API, (Api.ApiOptions) null, (zzda) new com.google.android.gms.common.api.internal.zzg());
    }

    @Hide
    public SettingsClient(@NonNull Context context) {
        super(context, LocationServices.API, (Api.ApiOptions) null, new com.google.android.gms.common.api.internal.zzg());
    }

    public Task<LocationSettingsResponse> checkLocationSettings(LocationSettingsRequest locationSettingsRequest) {
        return zzbj.zza(LocationServices.SettingsApi.checkLocationSettings(zzahw(), locationSettingsRequest), new LocationSettingsResponse());
    }
}
