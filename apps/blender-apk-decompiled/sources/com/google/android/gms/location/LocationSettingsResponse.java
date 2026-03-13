package com.google.android.gms.location;

import com.google.android.gms.common.api.Response;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
public class LocationSettingsResponse extends Response<LocationSettingsResult> {
    @Hide
    public LocationSettingsResponse() {
    }

    public LocationSettingsStates getLocationSettingsStates() {
        return getResult().getLocationSettingsStates();
    }
}
