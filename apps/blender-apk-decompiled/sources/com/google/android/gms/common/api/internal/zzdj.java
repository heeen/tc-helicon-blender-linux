package com.google.android.gms.common.api.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

/* JADX INFO: loaded from: classes.dex */
final class zzdj extends Handler {
    private /* synthetic */ zzdh zzgbp;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public zzdj(zzdh zzdhVar, Looper looper) {
        super(looper);
        this.zzgbp = zzdhVar;
    }

    @Override // android.os.Handler
    public final void handleMessage(Message message) {
        zzdh zzdhVar;
        Status status;
        switch (message.what) {
            case 0:
                PendingResult<?> pendingResult = (PendingResult) message.obj;
                synchronized (this.zzgbp.zzfvc) {
                    try {
                        if (pendingResult == null) {
                            zzdhVar = this.zzgbp.zzgbi;
                            status = new Status(13, "Transform returned null");
                        } else if (!(pendingResult instanceof zzct)) {
                            this.zzgbp.zzgbi.zza(pendingResult);
                        } else {
                            zzdhVar = this.zzgbp.zzgbi;
                            status = ((zzct) pendingResult).getStatus();
                        }
                        zzdhVar.zzd(status);
                    } catch (Throwable th) {
                        throw th;
                    }
                    break;
                }
                return;
            case 1:
                RuntimeException runtimeException = (RuntimeException) message.obj;
                String strValueOf = String.valueOf(runtimeException.getMessage());
                Log.e("TransformedResultImpl", strValueOf.length() != 0 ? "Runtime exception on the transformation worker thread: ".concat(strValueOf) : new String("Runtime exception on the transformation worker thread: "));
                throw runtimeException;
            default:
                int i = message.what;
                StringBuilder sb = new StringBuilder(70);
                sb.append("TransformationResultHandler received unknown message type: ");
                sb.append(i);
                Log.e("TransformedResultImpl", sb.toString());
                return;
        }
    }
}
