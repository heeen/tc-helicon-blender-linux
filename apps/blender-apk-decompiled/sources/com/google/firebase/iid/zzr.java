package com.google.firebase.iid;

import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
final class zzr {
    private final Messenger zzinb;
    private final zzi zzolc;

    zzr(IBinder iBinder) throws RemoteException {
        String interfaceDescriptor = iBinder.getInterfaceDescriptor();
        if ("android.os.IMessenger".equals(interfaceDescriptor)) {
            this.zzinb = new Messenger(iBinder);
            this.zzolc = null;
        } else if ("com.google.android.gms.iid.IMessengerCompat".equals(interfaceDescriptor)) {
            this.zzolc = new zzi(iBinder);
            this.zzinb = null;
        } else {
            String strValueOf = String.valueOf(interfaceDescriptor);
            Log.w("MessengerIpcClient", strValueOf.length() != 0 ? "Invalid interface descriptor: ".concat(strValueOf) : new String("Invalid interface descriptor: "));
            throw new RemoteException();
        }
    }

    final void send(Message message) throws RemoteException {
        if (this.zzinb != null) {
            this.zzinb.send(message);
        } else {
            if (this.zzolc == null) {
                throw new IllegalStateException("Both messengers are null");
            }
            this.zzolc.send(message);
        }
    }
}
