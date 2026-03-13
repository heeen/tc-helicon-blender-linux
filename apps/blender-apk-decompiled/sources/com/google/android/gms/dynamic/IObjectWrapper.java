package com.google.android.gms.dynamic;

import android.os.IBinder;
import android.os.IInterface;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzew;

/* JADX INFO: loaded from: classes.dex */
@Hide
public interface IObjectWrapper extends IInterface {

    public static abstract class zza extends zzew implements IObjectWrapper {
        public zza() {
            attachInterface(this, "com.google.android.gms.dynamic.IObjectWrapper");
        }

        public static IObjectWrapper zzaq(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.gms.dynamic.IObjectWrapper");
            return iInterfaceQueryLocalInterface instanceof IObjectWrapper ? (IObjectWrapper) iInterfaceQueryLocalInterface : new zzm(iBinder);
        }
    }
}
