package com.google.android.gms.internal;

import android.os.IInterface;
import android.os.RemoteException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;

/* JADX INFO: loaded from: classes.dex */
public interface zzbfp extends IInterface {
    void zza(Status status, long j) throws RemoteException;

    void zza(Status status, com.google.android.gms.clearcut.zzc zzcVar) throws RemoteException;

    void zza(Status status, com.google.android.gms.clearcut.zze[] zzeVarArr) throws RemoteException;

    void zza(DataHolder dataHolder) throws RemoteException;

    void zzb(Status status, long j) throws RemoteException;

    void zzb(Status status, com.google.android.gms.clearcut.zzc zzcVar) throws RemoteException;

    void zzo(Status status) throws RemoteException;

    void zzp(Status status) throws RemoteException;

    void zzq(Status status) throws RemoteException;
}
