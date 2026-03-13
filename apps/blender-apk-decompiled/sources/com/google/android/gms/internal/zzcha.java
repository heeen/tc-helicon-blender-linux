package com.google.android.gms.internal;

import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.Context;
import android.location.Location;
import android.os.RemoteException;
import com.google.android.gms.common.api.internal.zzci;
import com.google.android.gms.common.api.internal.zzck;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzcha {
    private final Context mContext;
    private final zzchr<zzcgw> zzitk;
    private ContentProviderClient zziuc = null;
    private boolean zziud = false;
    private final Map<zzck<LocationListener>, zzchf> zziue = new HashMap();
    private final Map<zzck<Object>, zzche> zziuf = new HashMap();
    private final Map<zzck<LocationCallback>, zzchb> zziug = new HashMap();

    public zzcha(Context context, zzchr<zzcgw> zzchrVar) {
        this.mContext = context;
        this.zzitk = zzchrVar;
    }

    private final zzchf zzm(zzci<LocationListener> zzciVar) {
        zzchf zzchfVar;
        synchronized (this.zziue) {
            zzchfVar = this.zziue.get(zzciVar.zzakx());
            if (zzchfVar == null) {
                zzchfVar = new zzchf(zzciVar);
            }
            this.zziue.put(zzciVar.zzakx(), zzchfVar);
        }
        return zzchfVar;
    }

    private final zzchb zzn(zzci<LocationCallback> zzciVar) {
        zzchb zzchbVar;
        synchronized (this.zziug) {
            zzchbVar = this.zziug.get(zzciVar.zzakx());
            if (zzchbVar == null) {
                zzchbVar = new zzchb(zzciVar);
            }
            this.zziug.put(zzciVar.zzakx(), zzchbVar);
        }
        return zzchbVar;
    }

    public final Location getLastLocation() throws RemoteException {
        this.zzitk.zzalv();
        return ((zzcgw) this.zzitk.zzalw()).zzim(this.mContext.getPackageName());
    }

    public final void removeAllListeners() throws RemoteException {
        synchronized (this.zziue) {
            for (zzchf zzchfVar : this.zziue.values()) {
                if (zzchfVar != null) {
                    ((zzcgw) this.zzitk.zzalw()).zza(zzchn.zza(zzchfVar, (zzcgr) null));
                }
            }
            this.zziue.clear();
        }
        synchronized (this.zziug) {
            for (zzchb zzchbVar : this.zziug.values()) {
                if (zzchbVar != null) {
                    ((zzcgw) this.zzitk.zzalw()).zza(zzchn.zza(zzchbVar, (zzcgr) null));
                }
            }
            this.zziug.clear();
        }
        synchronized (this.zziuf) {
            for (zzche zzcheVar : this.zziuf.values()) {
                if (zzcheVar != null) {
                    ((zzcgw) this.zzitk.zzalw()).zza(new zzcfw(2, null, zzcheVar.asBinder(), null));
                }
            }
            this.zziuf.clear();
        }
    }

    public final void zza(PendingIntent pendingIntent, zzcgr zzcgrVar) throws RemoteException {
        this.zzitk.zzalv();
        ((zzcgw) this.zzitk.zzalw()).zza(new zzchn(2, null, null, pendingIntent, null, zzcgrVar != null ? zzcgrVar.asBinder() : null));
    }

    public final void zza(zzck<LocationListener> zzckVar, zzcgr zzcgrVar) throws RemoteException {
        this.zzitk.zzalv();
        zzbq.checkNotNull(zzckVar, "Invalid null listener key");
        synchronized (this.zziue) {
            zzchf zzchfVarRemove = this.zziue.remove(zzckVar);
            if (zzchfVarRemove != null) {
                zzchfVarRemove.release();
                ((zzcgw) this.zzitk.zzalw()).zza(zzchn.zza(zzchfVarRemove, zzcgrVar));
            }
        }
    }

    public final void zza(zzcgr zzcgrVar) throws RemoteException {
        this.zzitk.zzalv();
        ((zzcgw) this.zzitk.zzalw()).zza(zzcgrVar);
    }

    public final void zza(zzchl zzchlVar, zzci<LocationCallback> zzciVar, zzcgr zzcgrVar) throws RemoteException {
        this.zzitk.zzalv();
        ((zzcgw) this.zzitk.zzalw()).zza(new zzchn(1, zzchlVar, null, null, zzn(zzciVar).asBinder(), zzcgrVar != null ? zzcgrVar.asBinder() : null));
    }

    public final void zza(LocationRequest locationRequest, PendingIntent pendingIntent, zzcgr zzcgrVar) throws RemoteException {
        this.zzitk.zzalv();
        ((zzcgw) this.zzitk.zzalw()).zza(new zzchn(1, zzchl.zza(locationRequest), null, pendingIntent, null, zzcgrVar != null ? zzcgrVar.asBinder() : null));
    }

    public final void zza(LocationRequest locationRequest, zzci<LocationListener> zzciVar, zzcgr zzcgrVar) throws RemoteException {
        this.zzitk.zzalv();
        ((zzcgw) this.zzitk.zzalw()).zza(new zzchn(1, zzchl.zza(locationRequest), zzm(zzciVar).asBinder(), null, null, zzcgrVar != null ? zzcgrVar.asBinder() : null));
    }

    public final LocationAvailability zzaxb() throws RemoteException {
        this.zzitk.zzalv();
        return ((zzcgw) this.zzitk.zzalw()).zzin(this.mContext.getPackageName());
    }

    public final void zzaxc() throws RemoteException {
        if (this.zziud) {
            zzbo(false);
        }
    }

    public final void zzb(zzck<LocationCallback> zzckVar, zzcgr zzcgrVar) throws RemoteException {
        this.zzitk.zzalv();
        zzbq.checkNotNull(zzckVar, "Invalid null listener key");
        synchronized (this.zziug) {
            zzchb zzchbVarRemove = this.zziug.remove(zzckVar);
            if (zzchbVarRemove != null) {
                zzchbVarRemove.release();
                ((zzcgw) this.zzitk.zzalw()).zza(zzchn.zza(zzchbVarRemove, zzcgrVar));
            }
        }
    }

    public final void zzbo(boolean z) throws RemoteException {
        this.zzitk.zzalv();
        ((zzcgw) this.zzitk.zzalw()).zzbo(z);
        this.zziud = z;
    }

    public final void zzc(Location location) throws RemoteException {
        this.zzitk.zzalv();
        ((zzcgw) this.zzitk.zzalw()).zzc(location);
    }
}
