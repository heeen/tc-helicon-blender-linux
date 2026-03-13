package com.google.android.gms.dynamic;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.google.android.gms.internal.zzew;
import com.google.android.gms.internal.zzex;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzl extends zzew implements zzk {
    public zzl() {
        attachInterface(this, "com.google.android.gms.dynamic.IFragmentWrapper");
    }

    @Override // android.os.Binder
    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        IInterface iInterfaceZzarh;
        int id;
        boolean retainInstance;
        if (zza(i, parcel, parcel2, i2)) {
            return true;
        }
        IObjectWrapper zzmVar = null;
        switch (i) {
            case 2:
                iInterfaceZzarh = zzarh();
                parcel2.writeNoException();
                zzex.zza(parcel2, iInterfaceZzarh);
                break;
            case 3:
                Bundle arguments = getArguments();
                parcel2.writeNoException();
                zzex.zzb(parcel2, arguments);
                break;
            case 4:
                id = getId();
                parcel2.writeNoException();
                parcel2.writeInt(id);
                break;
            case 5:
                iInterfaceZzarh = zzari();
                parcel2.writeNoException();
                zzex.zza(parcel2, iInterfaceZzarh);
                break;
            case 6:
                iInterfaceZzarh = zzarj();
                parcel2.writeNoException();
                zzex.zza(parcel2, iInterfaceZzarh);
                break;
            case 7:
                retainInstance = getRetainInstance();
                parcel2.writeNoException();
                zzex.zza(parcel2, retainInstance);
                break;
            case 8:
                String tag = getTag();
                parcel2.writeNoException();
                parcel2.writeString(tag);
                break;
            case 9:
                iInterfaceZzarh = zzark();
                parcel2.writeNoException();
                zzex.zza(parcel2, iInterfaceZzarh);
                break;
            case 10:
                id = getTargetRequestCode();
                parcel2.writeNoException();
                parcel2.writeInt(id);
                break;
            case 11:
                retainInstance = getUserVisibleHint();
                parcel2.writeNoException();
                zzex.zza(parcel2, retainInstance);
                break;
            case 12:
                iInterfaceZzarh = getView();
                parcel2.writeNoException();
                zzex.zza(parcel2, iInterfaceZzarh);
                break;
            case 13:
                retainInstance = isAdded();
                parcel2.writeNoException();
                zzex.zza(parcel2, retainInstance);
                break;
            case 14:
                retainInstance = isDetached();
                parcel2.writeNoException();
                zzex.zza(parcel2, retainInstance);
                break;
            case 15:
                retainInstance = isHidden();
                parcel2.writeNoException();
                zzex.zza(parcel2, retainInstance);
                break;
            case 16:
                retainInstance = isInLayout();
                parcel2.writeNoException();
                zzex.zza(parcel2, retainInstance);
                break;
            case 17:
                retainInstance = isRemoving();
                parcel2.writeNoException();
                zzex.zza(parcel2, retainInstance);
                break;
            case 18:
                retainInstance = isResumed();
                parcel2.writeNoException();
                zzex.zza(parcel2, retainInstance);
                break;
            case 19:
                retainInstance = isVisible();
                parcel2.writeNoException();
                zzex.zza(parcel2, retainInstance);
                break;
            case 20:
                IBinder strongBinder = parcel.readStrongBinder();
                if (strongBinder != null) {
                    IInterface iInterfaceQueryLocalInterface = strongBinder.queryLocalInterface("com.google.android.gms.dynamic.IObjectWrapper");
                    zzmVar = iInterfaceQueryLocalInterface instanceof IObjectWrapper ? (IObjectWrapper) iInterfaceQueryLocalInterface : new zzm(strongBinder);
                }
                zzw(zzmVar);
                parcel2.writeNoException();
                break;
            case 21:
                setHasOptionsMenu(zzex.zza(parcel));
                parcel2.writeNoException();
                break;
            case 22:
                setMenuVisibility(zzex.zza(parcel));
                parcel2.writeNoException();
                break;
            case 23:
                setRetainInstance(zzex.zza(parcel));
                parcel2.writeNoException();
                break;
            case 24:
                setUserVisibleHint(zzex.zza(parcel));
                parcel2.writeNoException();
                break;
            case 25:
                startActivity((Intent) zzex.zza(parcel, Intent.CREATOR));
                parcel2.writeNoException();
                break;
            case 26:
                startActivityForResult((Intent) zzex.zza(parcel, Intent.CREATOR), parcel.readInt());
                parcel2.writeNoException();
                break;
            case 27:
                IBinder strongBinder2 = parcel.readStrongBinder();
                if (strongBinder2 != null) {
                    IInterface iInterfaceQueryLocalInterface2 = strongBinder2.queryLocalInterface("com.google.android.gms.dynamic.IObjectWrapper");
                    zzmVar = iInterfaceQueryLocalInterface2 instanceof IObjectWrapper ? (IObjectWrapper) iInterfaceQueryLocalInterface2 : new zzm(strongBinder2);
                }
                zzx(zzmVar);
                parcel2.writeNoException();
                break;
        }
        return true;
    }
}
