package com.google.firebase.iid;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.common.internal.zzbq;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
final class zzm implements ServiceConnection {
    int state;
    final Messenger zzing;
    final Queue<zzt<?>> zzini;
    final SparseArray<zzt<?>> zzinj;
    zzr zzoky;
    final /* synthetic */ zzk zzokz;

    private zzm(zzk zzkVar) {
        this.zzokz = zzkVar;
        this.state = 0;
        this.zzing = new Messenger(new Handler(Looper.getMainLooper(), new Handler.Callback(this) { // from class: com.google.firebase.iid.zzn
            private final zzm zzola;

            {
                this.zzola = this;
            }

            @Override // android.os.Handler.Callback
            public final boolean handleMessage(Message message) {
                return this.zzola.zzc(message);
            }
        }));
        this.zzini = new ArrayDeque();
        this.zzinj = new SparseArray<>();
    }

    private final void zza(zzu zzuVar) {
        Iterator<zzt<?>> it = this.zzini.iterator();
        while (it.hasNext()) {
            it.next().zzb(zzuVar);
        }
        this.zzini.clear();
        for (int i = 0; i < this.zzinj.size(); i++) {
            this.zzinj.valueAt(i).zzb(zzuVar);
        }
        this.zzinj.clear();
    }

    private final void zzawt() {
        this.zzokz.zzind.execute(new Runnable(this) { // from class: com.google.firebase.iid.zzp
            private final zzm zzola;

            {
                this.zzola = this;
            }

            @Override // java.lang.Runnable
            public final void run() {
                final zzt<?> zztVarPoll;
                final zzm zzmVar = this.zzola;
                while (true) {
                    synchronized (zzmVar) {
                        if (zzmVar.state != 2) {
                            return;
                        }
                        if (zzmVar.zzini.isEmpty()) {
                            zzmVar.zzawu();
                            return;
                        } else {
                            zztVarPoll = zzmVar.zzini.poll();
                            zzmVar.zzinj.put(zztVarPoll.zzino, zztVarPoll);
                            zzmVar.zzokz.zzind.schedule(new Runnable(zzmVar, zztVarPoll) { // from class: com.google.firebase.iid.zzq
                                private final zzm zzola;
                                private final zzt zzolb;

                                {
                                    this.zzola = zzmVar;
                                    this.zzolb = zztVarPoll;
                                }

                                @Override // java.lang.Runnable
                                public final void run() {
                                    this.zzola.zzec(this.zzolb.zzino);
                                }
                            }, 30L, TimeUnit.SECONDS);
                        }
                    }
                    if (Log.isLoggable("MessengerIpcClient", 3)) {
                        String strValueOf = String.valueOf(zztVarPoll);
                        StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 8);
                        sb.append("Sending ");
                        sb.append(strValueOf);
                        Log.d("MessengerIpcClient", sb.toString());
                    }
                    Context context = zzmVar.zzokz.zzaiq;
                    Messenger messenger = zzmVar.zzing;
                    Message messageObtain = Message.obtain();
                    messageObtain.what = zztVarPoll.what;
                    messageObtain.arg1 = zztVarPoll.zzino;
                    messageObtain.replyTo = messenger;
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("oneWay", zztVarPoll.zzaww());
                    bundle.putString("pkg", context.getPackageName());
                    bundle.putBundle("data", zztVarPoll.zzinp);
                    messageObtain.setData(bundle);
                    try {
                        zzmVar.zzoky.send(messageObtain);
                    } catch (RemoteException e) {
                        zzmVar.zzl(2, e.getMessage());
                    }
                }
            }
        });
    }

    @Override // android.content.ServiceConnection
    public final synchronized void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (Log.isLoggable("MessengerIpcClient", 2)) {
            Log.v("MessengerIpcClient", "Service connected");
        }
        if (iBinder == null) {
            zzl(0, "Null service connection");
            return;
        }
        try {
            this.zzoky = new zzr(iBinder);
            this.state = 2;
            zzawt();
        } catch (RemoteException e) {
            zzl(0, e.getMessage());
        }
    }

    @Override // android.content.ServiceConnection
    public final synchronized void onServiceDisconnected(ComponentName componentName) {
        if (Log.isLoggable("MessengerIpcClient", 2)) {
            Log.v("MessengerIpcClient", "Service disconnected");
        }
        zzl(2, "Service disconnected");
    }

    final synchronized void zzawu() {
        if (this.state == 2 && this.zzini.isEmpty() && this.zzinj.size() == 0) {
            if (Log.isLoggable("MessengerIpcClient", 2)) {
                Log.v("MessengerIpcClient", "Finished handling requests, unbinding");
            }
            this.state = 3;
            com.google.android.gms.common.stats.zza.zzanm();
            this.zzokz.zzaiq.unbindService(this);
        }
    }

    final synchronized void zzawv() {
        if (this.state == 1) {
            zzl(1, "Timed out while binding");
        }
    }

    final synchronized boolean zzb(zzt zztVar) {
        switch (this.state) {
            case 0:
                this.zzini.add(zztVar);
                zzbq.checkState(this.state == 0);
                if (Log.isLoggable("MessengerIpcClient", 2)) {
                    Log.v("MessengerIpcClient", "Starting bind to GmsCore");
                }
                this.state = 1;
                Intent intent = new Intent("com.google.android.c2dm.intent.REGISTER");
                intent.setPackage("com.google.android.gms");
                if (com.google.android.gms.common.stats.zza.zzanm().zza(this.zzokz.zzaiq, intent, this, 1)) {
                    this.zzokz.zzind.schedule(new Runnable(this) { // from class: com.google.firebase.iid.zzo
                        private final zzm zzola;

                        {
                            this.zzola = this;
                        }

                        @Override // java.lang.Runnable
                        public final void run() {
                            this.zzola.zzawv();
                        }
                    }, 30L, TimeUnit.SECONDS);
                } else {
                    zzl(0, "Unable to bind to service");
                }
                return true;
            case 1:
                this.zzini.add(zztVar);
                return true;
            case 2:
                this.zzini.add(zztVar);
                zzawt();
                return true;
            case 3:
            case 4:
                return false;
            default:
                int i = this.state;
                StringBuilder sb = new StringBuilder(26);
                sb.append("Unknown state: ");
                sb.append(i);
                throw new IllegalStateException(sb.toString());
        }
    }

    final boolean zzc(Message message) {
        int i = message.arg1;
        if (Log.isLoggable("MessengerIpcClient", 3)) {
            StringBuilder sb = new StringBuilder(41);
            sb.append("Received response to request: ");
            sb.append(i);
            Log.d("MessengerIpcClient", sb.toString());
        }
        synchronized (this) {
            zzt<?> zztVar = this.zzinj.get(i);
            if (zztVar == null) {
                StringBuilder sb2 = new StringBuilder(50);
                sb2.append("Received response for unknown request: ");
                sb2.append(i);
                Log.w("MessengerIpcClient", sb2.toString());
                return true;
            }
            this.zzinj.remove(i);
            zzawu();
            Bundle data = message.getData();
            if (data.getBoolean("unsupported", false)) {
                zztVar.zzb(new zzu(4, "Not supported by GmsCore"));
            } else {
                zztVar.zzx(data);
            }
            return true;
        }
    }

    final synchronized void zzec(int i) {
        zzt<?> zztVar = this.zzinj.get(i);
        if (zztVar != null) {
            StringBuilder sb = new StringBuilder(31);
            sb.append("Timing out request: ");
            sb.append(i);
            Log.w("MessengerIpcClient", sb.toString());
            this.zzinj.remove(i);
            zztVar.zzb(new zzu(3, "Timed out waiting for response"));
            zzawu();
        }
    }

    final synchronized void zzl(int i, String str) {
        if (Log.isLoggable("MessengerIpcClient", 3)) {
            String strValueOf = String.valueOf(str);
            Log.d("MessengerIpcClient", strValueOf.length() != 0 ? "Disconnected: ".concat(strValueOf) : new String("Disconnected: "));
        }
        switch (this.state) {
            case 0:
                throw new IllegalStateException();
            case 1:
            case 2:
                if (Log.isLoggable("MessengerIpcClient", 2)) {
                    Log.v("MessengerIpcClient", "Unbinding service");
                }
                this.state = 4;
                com.google.android.gms.common.stats.zza.zzanm();
                this.zzokz.zzaiq.unbindService(this);
                zza(new zzu(i, str));
                return;
            case 3:
                this.state = 4;
                return;
            case 4:
                return;
            default:
                int i2 = this.state;
                StringBuilder sb = new StringBuilder(26);
                sb.append("Unknown state: ");
                sb.append(i2);
                throw new IllegalStateException(sb.toString());
        }
    }
}
