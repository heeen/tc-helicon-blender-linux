package com.google.android.gms.internal;

import android.os.IInterface;
import android.os.RemoteException;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.location.LocationSettingsResult;

/* JADX INFO: loaded from: classes.dex */
@Hide
public interface zzcgy extends IInterface {
    void zza(LocationSettingsResult locationSettingsResult) throws RemoteException;
}
