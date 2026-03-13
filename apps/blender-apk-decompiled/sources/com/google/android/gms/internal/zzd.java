package com.google.android.gms.internal;

import android.os.Process;
import java.util.concurrent.BlockingQueue;

/* JADX INFO: loaded from: classes.dex */
public final class zzd extends Thread {
    private static final boolean DEBUG = zzaf.DEBUG;
    private final BlockingQueue<zzr<?>> zzh;
    private final BlockingQueue<zzr<?>> zzi;
    private final zzb zzj;
    private final zzaa zzk;
    private volatile boolean zzl = false;
    private final zzf zzm = new zzf(this);

    public zzd(BlockingQueue<zzr<?>> blockingQueue, BlockingQueue<zzr<?>> blockingQueue2, zzb zzbVar, zzaa zzaaVar) {
        this.zzh = blockingQueue;
        this.zzi = blockingQueue2;
        this.zzj = zzbVar;
        this.zzk = zzaaVar;
    }

    private final void processRequest() throws InterruptedException {
        zzr<?> zzrVarTake = this.zzh.take();
        zzrVarTake.zzb("cache-queue-take");
        zzrVarTake.isCanceled();
        zzc zzcVarZza = this.zzj.zza(zzrVarTake.getUrl());
        if (zzcVarZza == null) {
            zzrVarTake.zzb("cache-miss");
            if (this.zzm.zzb(zzrVarTake)) {
                return;
            }
            this.zzi.put(zzrVarTake);
            return;
        }
        if (zzcVarZza.zza()) {
            zzrVarTake.zzb("cache-hit-expired");
            zzrVarTake.zza(zzcVarZza);
            if (this.zzm.zzb(zzrVarTake)) {
                return;
            }
            this.zzi.put(zzrVarTake);
            return;
        }
        zzrVarTake.zzb("cache-hit");
        zzx<?> zzxVarZza = zzrVarTake.zza(new zzp(zzcVarZza.data, zzcVarZza.zzf));
        zzrVarTake.zzb("cache-hit-parsed");
        if (zzcVarZza.zze < System.currentTimeMillis()) {
            zzrVarTake.zzb("cache-hit-refresh-needed");
            zzrVarTake.zza(zzcVarZza);
            zzxVarZza.zzbi = true;
            if (!this.zzm.zzb(zzrVarTake)) {
                this.zzk.zza(zzrVarTake, zzxVarZza, new zze(this, zzrVarTake));
                return;
            }
        }
        this.zzk.zzb(zzrVarTake, zzxVarZza);
    }

    public final void quit() {
        this.zzl = true;
        interrupt();
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public final void run() {
        if (DEBUG) {
            zzaf.zza("start new dispatcher", new Object[0]);
        }
        Process.setThreadPriority(10);
        this.zzj.initialize();
        while (true) {
            try {
                processRequest();
            } catch (InterruptedException unused) {
                if (this.zzl) {
                    return;
                }
            }
        }
    }
}
