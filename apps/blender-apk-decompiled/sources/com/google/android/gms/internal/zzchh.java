package com.google.android.gms.internal;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.zzci;
import com.google.android.gms.common.api.internal.zzck;
import com.google.android.gms.common.api.internal.zzcz;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzchh extends zzcfq {
    private final zzcha zziuk;

    public zzchh(Context context, Looper looper, GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener, String str) {
        this(context, looper, connectionCallbacks, onConnectionFailedListener, str, com.google.android.gms.common.internal.zzr.zzcm(context));
    }

    public zzchh(Context context, Looper looper, GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener, String str, com.google.android.gms.common.internal.zzr zzrVar) {
        super(context, looper, connectionCallbacks, onConnectionFailedListener, str, zzrVar);
        this.zziuk = new zzcha(context, this.zzitk);
    }

    @Override // com.google.android.gms.common.internal.zzd, com.google.android.gms.common.api.Api.zze
    public final void disconnect() {
        synchronized (this.zziuk) {
            if (isConnected()) {
                try {
                    this.zziuk.removeAllListeners();
                    this.zziuk.zzaxc();
                } catch (Exception e) {
                    Log.e("LocationClientImpl", "Client disconnected before listeners could be cleaned up", e);
                }
                super.disconnect();
            } else {
                super.disconnect();
            }
        }
    }

    public final Location getLastLocation() throws RemoteException {
        return this.zziuk.getLastLocation();
    }

    public final void zza(long j, PendingIntent pendingIntent) throws RemoteException {
        zzalv();
        zzbq.checkNotNull(pendingIntent);
        zzbq.checkArgument(j >= 0, "detectionIntervalMillis must be >= 0");
        ((zzcgw) zzalw()).zza(j, true, pendingIntent);
    }

    public final void zza(PendingIntent pendingIntent, com.google.android.gms.common.api.internal.zzn<Status> zznVar) throws RemoteException {
        zzalv();
        zzbq.checkNotNull(zznVar, "ResultHolder not provided.");
        ((zzcgw) zzalw()).zza(pendingIntent, new zzcz(zznVar));
    }

    public final void zza(PendingIntent pendingIntent, zzcgr zzcgrVar) throws RemoteException {
        this.zziuk.zza(pendingIntent, zzcgrVar);
    }

    public final void zza(zzck<LocationListener> zzckVar, zzcgr zzcgrVar) throws RemoteException {
        this.zziuk.zza(zzckVar, zzcgrVar);
    }

    public final void zza(zzcgr zzcgrVar) throws RemoteException {
        this.zziuk.zza(zzcgrVar);
    }

    public final void zza(zzchl zzchlVar, zzci<LocationCallback> zzciVar, zzcgr zzcgrVar) throws RemoteException {
        synchronized (this.zziuk) {
            this.zziuk.zza(zzchlVar, zzciVar, zzcgrVar);
        }
    }

    public final void zza(ActivityTransitionRequest activityTransitionRequest, PendingIntent pendingIntent, com.google.android.gms.common.api.internal.zzn<Status> zznVar) throws RemoteException {
        zzalv();
        zzbq.checkNotNull(zznVar, "ResultHolder not provided.");
        ((zzcgw) zzalw()).zza(activityTransitionRequest, pendingIntent, new zzcz(zznVar));
    }

    public final void zza(GeofencingRequest geofencingRequest, PendingIntent pendingIntent, com.google.android.gms.common.api.internal.zzn<Status> zznVar) throws RemoteException {
        zzalv();
        zzbq.checkNotNull(geofencingRequest, "geofencingRequest can't be null.");
        zzbq.checkNotNull(pendingIntent, "PendingIntent must be specified.");
        zzbq.checkNotNull(zznVar, "ResultHolder not provided.");
        ((zzcgw) zzalw()).zza(geofencingRequest, pendingIntent, new zzchi(zznVar));
    }

    public final void zza(LocationRequest locationRequest, PendingIntent pendingIntent, zzcgr zzcgrVar) throws RemoteException {
        this.zziuk.zza(locationRequest, pendingIntent, zzcgrVar);
    }

    public final void zza(LocationRequest locationRequest, zzci<LocationListener> zzciVar, zzcgr zzcgrVar) throws RemoteException {
        synchronized (this.zziuk) {
            this.zziuk.zza(locationRequest, zzciVar, zzcgrVar);
        }
    }

    public final void zza(LocationSettingsRequest locationSettingsRequest, com.google.android.gms.common.api.internal.zzn<LocationSettingsResult> zznVar, String str) throws RemoteException {
        zzalv();
        zzbq.checkArgument(locationSettingsRequest != null, "locationSettingsRequest can't be null nor empty.");
        zzbq.checkArgument(zznVar != null, "listener can't be null.");
        ((zzcgw) zzalw()).zza(locationSettingsRequest, new zzchk(zznVar), str);
    }

    public final void zza(com.google.android.gms.location.zzal zzalVar, com.google.android.gms.common.api.internal.zzn<Status> zznVar) throws RemoteException {
        zzalv();
        zzbq.checkNotNull(zzalVar, "removeGeofencingRequest can't be null.");
        zzbq.checkNotNull(zznVar, "ResultHolder not provided.");
        ((zzcgw) zzalw()).zza(zzalVar, new zzchj(zznVar));
    }

    public final LocationAvailability zzaxb() throws RemoteException {
        return this.zziuk.zzaxb();
    }

    public final void zzb(zzck<LocationCallback> zzckVar, zzcgr zzcgrVar) throws RemoteException {
        this.zziuk.zzb(zzckVar, zzcgrVar);
    }

    public final void zzbo(boolean z) throws RemoteException {
        this.zziuk.zzbo(z);
    }

    public final void zzc(PendingIntent pendingIntent) throws RemoteException {
        zzalv();
        zzbq.checkNotNull(pendingIntent);
        ((zzcgw) zzalw()).zzc(pendingIntent);
    }

    public final void zzc(Location location) throws RemoteException {
        this.zziuk.zzc(location);
    }
}
