package com.google.firebase.iid;

import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public class zzi implements Parcelable {
    public static final Parcelable.Creator<zzi> CREATOR = new zzj();
    private Messenger zzinb;
    private com.google.android.gms.iid.zzi zzinc;

    public static final class zza extends ClassLoader {
        @Override // java.lang.ClassLoader
        protected final Class<?> loadClass(String str, boolean z) throws ClassNotFoundException {
            if (!"com.google.android.gms.iid.MessengerCompat".equals(str)) {
                return super.loadClass(str, z);
            }
            if (!FirebaseInstanceId.zzclf()) {
                return zzi.class;
            }
            Log.d("FirebaseInstanceId", "Using renamed FirebaseIidMessengerCompat class");
            return zzi.class;
        }
    }

    public zzi(IBinder iBinder) {
        if (Build.VERSION.SDK_INT >= 21) {
            this.zzinb = new Messenger(iBinder);
        } else {
            this.zzinc = com.google.android.gms.iid.zzj.zzbc(iBinder);
        }
    }

    private final IBinder getBinder() {
        return this.zzinb != null ? this.zzinb.getBinder() : this.zzinc.asBinder();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        try {
            return getBinder().equals(((zzi) obj).getBinder());
        } catch (ClassCastException unused) {
            return false;
        }
    }

    public int hashCode() {
        return getBinder().hashCode();
    }

    public final void send(Message message) throws RemoteException {
        if (this.zzinb != null) {
            this.zzinb.send(message);
        } else {
            this.zzinc.send(message);
        }
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(this.zzinb != null ? this.zzinb.getBinder() : this.zzinc.asBinder());
    }
}
