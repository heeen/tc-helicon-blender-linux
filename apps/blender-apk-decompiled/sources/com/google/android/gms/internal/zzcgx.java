package com.google.android.gms.internal;

import android.app.PendingIntent;
import android.location.Location;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.common.api.internal.zzca;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationSettingsRequest;

/* JADX INFO: loaded from: classes.dex */
public final class zzcgx extends zzev implements zzcgw {
    zzcgx(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.location.internal.IGoogleLocationManagerService");
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zza(long j, boolean z, PendingIntent pendingIntent) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        parcelZzbc.writeLong(j);
        zzex.zza(parcelZzbc, true);
        zzex.zza(parcelZzbc, pendingIntent);
        zzb(5, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zza(PendingIntent pendingIntent, zzca zzcaVar) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, pendingIntent);
        zzex.zza(parcelZzbc, zzcaVar);
        zzb(73, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zza(zzcfw zzcfwVar) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, zzcfwVar);
        zzb(75, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zza(zzcgr zzcgrVar) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, zzcgrVar);
        zzb(67, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zza(zzchn zzchnVar) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, zzchnVar);
        zzb(59, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zza(ActivityTransitionRequest activityTransitionRequest, PendingIntent pendingIntent, zzca zzcaVar) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, activityTransitionRequest);
        zzex.zza(parcelZzbc, pendingIntent);
        zzex.zza(parcelZzbc, zzcaVar);
        zzb(72, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zza(GeofencingRequest geofencingRequest, PendingIntent pendingIntent, zzcgu zzcguVar) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, geofencingRequest);
        zzex.zza(parcelZzbc, pendingIntent);
        zzex.zza(parcelZzbc, zzcguVar);
        zzb(57, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zza(LocationSettingsRequest locationSettingsRequest, zzcgy zzcgyVar, String str) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, locationSettingsRequest);
        zzex.zza(parcelZzbc, zzcgyVar);
        parcelZzbc.writeString(str);
        zzb(63, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zza(com.google.android.gms.location.zzal zzalVar, zzcgu zzcguVar) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, zzalVar);
        zzex.zza(parcelZzbc, zzcguVar);
        zzb(74, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zzbo(boolean z) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, z);
        zzb(12, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zzc(PendingIntent pendingIntent) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, pendingIntent);
        zzb(6, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final void zzc(Location location) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        zzex.zza(parcelZzbc, location);
        zzb(13, parcelZzbc);
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final Location zzim(String str) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        parcelZzbc.writeString(str);
        Parcel parcelZza = zza(21, parcelZzbc);
        Location location = (Location) zzex.zza(parcelZza, Location.CREATOR);
        parcelZza.recycle();
        return location;
    }

    @Override // com.google.android.gms.internal.zzcgw
    public final LocationAvailability zzin(String str) throws RemoteException {
        Parcel parcelZzbc = zzbc();
        parcelZzbc.writeString(str);
        Parcel parcelZza = zza(34, parcelZzbc);
        LocationAvailability locationAvailability = (LocationAvailability) zzex.zza(parcelZza, LocationAvailability.CREATOR);
        parcelZza.recycle();
        return locationAvailability;
    }
}
