package com.google.firebase.iid;

import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: loaded from: classes.dex */
abstract class zzt<T> {
    final int what;
    final TaskCompletionSource<T> zzgyc = new TaskCompletionSource<>();
    final int zzino;
    final Bundle zzinp;

    zzt(int i, int i2, Bundle bundle) {
        this.zzino = i;
        this.what = i2;
        this.zzinp = bundle;
    }

    final void finish(T t) {
        if (Log.isLoggable("MessengerIpcClient", 3)) {
            String strValueOf = String.valueOf(this);
            String strValueOf2 = String.valueOf(t);
            StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 16 + String.valueOf(strValueOf2).length());
            sb.append("Finishing ");
            sb.append(strValueOf);
            sb.append(" with ");
            sb.append(strValueOf2);
            Log.d("MessengerIpcClient", sb.toString());
        }
        this.zzgyc.setResult(t);
    }

    public String toString() {
        int i = this.what;
        int i2 = this.zzino;
        boolean zZzaww = zzaww();
        StringBuilder sb = new StringBuilder(55);
        sb.append("Request { what=");
        sb.append(i);
        sb.append(" id=");
        sb.append(i2);
        sb.append(" oneWay=");
        sb.append(zZzaww);
        sb.append("}");
        return sb.toString();
    }

    abstract boolean zzaww();

    final void zzb(zzu zzuVar) {
        if (Log.isLoggable("MessengerIpcClient", 3)) {
            String strValueOf = String.valueOf(this);
            String strValueOf2 = String.valueOf(zzuVar);
            StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 14 + String.valueOf(strValueOf2).length());
            sb.append("Failing ");
            sb.append(strValueOf);
            sb.append(" with ");
            sb.append(strValueOf2);
            Log.d("MessengerIpcClient", sb.toString());
        }
        this.zzgyc.setException(zzuVar);
    }

    abstract void zzx(Bundle bundle);
}
