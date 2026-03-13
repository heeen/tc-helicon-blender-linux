package com.google.android.gms.common;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzau;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzn extends zzbgl {
    public static final Parcelable.Creator<zzn> CREATOR = new zzo();
    private final String zzfri;
    private final zzh zzfrj;
    private final boolean zzfrk;

    zzn(String str, IBinder iBinder, boolean z) {
        this.zzfri = str;
        this.zzfrj = zzak(iBinder);
        this.zzfrk = z;
    }

    zzn(String str, zzh zzhVar, boolean z) {
        this.zzfri = str;
        this.zzfrj = zzhVar;
        this.zzfrk = z;
    }

    private static zzh zzak(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        try {
            IObjectWrapper iObjectWrapperZzahg = zzau.zzam(iBinder).zzahg();
            byte[] bArr = iObjectWrapperZzahg == null ? null : (byte[]) com.google.android.gms.dynamic.zzn.zzy(iObjectWrapperZzahg);
            if (bArr != null) {
                return new zzi(bArr);
            }
            Log.e("GoogleCertificatesQuery", "Could not unwrap certificate");
            return null;
        } catch (RemoteException e) {
            Log.e("GoogleCertificatesQuery", "Could not unwrap certificate", e);
            return null;
        }
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        IBinder iBinderAsBinder;
        int iZze = zzbgo.zze(parcel);
        zzbgo.zza(parcel, 1, this.zzfri, false);
        if (this.zzfrj == null) {
            Log.w("GoogleCertificatesQuery", "certificate binder is null");
            iBinderAsBinder = null;
        } else {
            iBinderAsBinder = this.zzfrj.asBinder();
        }
        zzbgo.zza(parcel, 2, iBinderAsBinder, false);
        zzbgo.zza(parcel, 3, this.zzfrk);
        zzbgo.zzai(parcel, iZze);
    }
}
