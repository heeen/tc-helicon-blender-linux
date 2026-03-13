package com.google.android.gms.internal;

import android.net.TrafficStats;
import android.os.Process;
import android.os.SystemClock;
import java.util.concurrent.BlockingQueue;

/* JADX INFO: loaded from: classes.dex */
public final class zzn extends Thread {
    private final zzm zzaa;
    private final zzb zzj;
    private final zzaa zzk;
    private volatile boolean zzl = false;
    private final BlockingQueue<zzr<?>> zzz;

    public zzn(BlockingQueue<zzr<?>> blockingQueue, zzm zzmVar, zzb zzbVar, zzaa zzaaVar) {
        this.zzz = blockingQueue;
        this.zzaa = zzmVar;
        this.zzj = zzbVar;
        this.zzk = zzaaVar;
    }

    private final void processRequest() throws InterruptedException {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        zzr<?> zzrVarTake = this.zzz.take();
        try {
            zzrVarTake.zzb("network-queue-take");
            zzrVarTake.isCanceled();
            TrafficStats.setThreadStatsTag(zzrVarTake.zzd());
            zzp zzpVarZzc = this.zzaa.zzc(zzrVarTake);
            zzrVarTake.zzb("network-http-complete");
            if (zzpVarZzc.zzac && zzrVarTake.zzk()) {
                zzrVarTake.zzc("not-modified");
                zzrVarTake.zzl();
                return;
            }
            zzx<?> zzxVarZza = zzrVarTake.zza(zzpVarZzc);
            zzrVarTake.zzb("network-parse-complete");
            if (zzrVarTake.zzg() && zzxVarZza.zzbg != null) {
                this.zzj.zza(zzrVarTake.getUrl(), zzxVarZza.zzbg);
                zzrVarTake.zzb("network-cache-written");
            }
            zzrVarTake.zzj();
            this.zzk.zzb(zzrVarTake, zzxVarZza);
            zzrVarTake.zza(zzxVarZza);
        } catch (zzae e) {
            e.zza(SystemClock.elapsedRealtime() - jElapsedRealtime);
            this.zzk.zza(zzrVarTake, e);
            zzrVarTake.zzl();
        } catch (Exception e2) {
            zzaf.zza(e2, "Unhandled exception %s", e2.toString());
            zzae zzaeVar = new zzae(e2);
            zzaeVar.zza(SystemClock.elapsedRealtime() - jElapsedRealtime);
            this.zzk.zza(zzrVarTake, zzaeVar);
            zzrVarTake.zzl();
        }
    }

    public final void quit() {
        this.zzl = true;
        interrupt();
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public final void run() {
        Process.setThreadPriority(10);
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
