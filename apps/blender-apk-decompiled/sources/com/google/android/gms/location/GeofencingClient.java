package com.google.android.gms.location;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.internal.zzda;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbj;
import com.google.android.gms.tasks.Task;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class GeofencingClient extends GoogleApi<Api.ApiOptions.NoOptions> {
    @Hide
    public GeofencingClient(@NonNull Activity activity) {
        super(activity, (Api<Api.ApiOptions>) LocationServices.API, (Api.ApiOptions) null, (zzda) new com.google.android.gms.common.api.internal.zzg());
    }

    @Hide
    public GeofencingClient(@NonNull Context context) {
        super(context, LocationServices.API, (Api.ApiOptions) null, new com.google.android.gms.common.api.internal.zzg());
    }

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    public Task<Void> addGeofences(GeofencingRequest geofencingRequest, PendingIntent pendingIntent) {
        return zzbj.zzb(LocationServices.GeofencingApi.addGeofences(zzahw(), geofencingRequest, pendingIntent));
    }

    public Task<Void> removeGeofences(PendingIntent pendingIntent) {
        return zzbj.zzb(LocationServices.GeofencingApi.removeGeofences(zzahw(), pendingIntent));
    }

    public Task<Void> removeGeofences(List<String> list) {
        return zzbj.zzb(LocationServices.GeofencingApi.removeGeofences(zzahw(), list));
    }
}
