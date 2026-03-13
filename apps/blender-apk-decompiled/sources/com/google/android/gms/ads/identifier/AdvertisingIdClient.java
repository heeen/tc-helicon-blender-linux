package com.google.android.gms.ads.identifier;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.annotation.KeepForSdkWithMembers;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.common.zzf;
import com.google.android.gms.internal.zzfp;
import com.google.android.gms.internal.zzfq;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
@KeepForSdkWithMembers
public class AdvertisingIdClient {
    private final Context mContext;

    @Nullable
    private com.google.android.gms.common.zza zzams;

    @Nullable
    private zzfp zzamt;
    private boolean zzamu;
    private Object zzamv;

    @Nullable
    private zza zzamw;
    private boolean zzamx;
    private long zzamy;

    public static final class Info {
        private final String zzane;
        private final boolean zzanf;

        public Info(String str, boolean z) {
            this.zzane = str;
            this.zzanf = z;
        }

        public final String getId() {
            return this.zzane;
        }

        public final boolean isLimitAdTrackingEnabled() {
            return this.zzanf;
        }

        public final String toString() {
            String str = this.zzane;
            boolean z = this.zzanf;
            StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 7);
            sb.append("{");
            sb.append(str);
            sb.append("}");
            sb.append(z);
            return sb.toString();
        }
    }

    @Hide
    static class zza extends Thread {
        private WeakReference<AdvertisingIdClient> zzana;
        private long zzanb;
        CountDownLatch zzanc = new CountDownLatch(1);
        boolean zzand = false;

        public zza(AdvertisingIdClient advertisingIdClient, long j) {
            this.zzana = new WeakReference<>(advertisingIdClient);
            this.zzanb = j;
            start();
        }

        private final void disconnect() {
            AdvertisingIdClient advertisingIdClient = this.zzana.get();
            if (advertisingIdClient != null) {
                advertisingIdClient.finish();
                this.zzand = true;
            }
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public final void run() {
            try {
                if (this.zzanc.await(this.zzanb, TimeUnit.MILLISECONDS)) {
                    return;
                }
                disconnect();
            } catch (InterruptedException unused) {
                disconnect();
            }
        }
    }

    @Hide
    public AdvertisingIdClient(Context context) {
        this(context, 30000L, false, false);
    }

    @Hide
    public AdvertisingIdClient(Context context, long j, boolean z, boolean z2) {
        Context applicationContext;
        this.zzamv = new Object();
        zzbq.checkNotNull(context);
        if (z && (applicationContext = context.getApplicationContext()) != null) {
            context = applicationContext;
        }
        this.mContext = context;
        this.zzamu = false;
        this.zzamy = j;
        this.zzamx = z2;
    }

    public static Info getAdvertisingIdInfo(Context context) throws GooglePlayServicesRepairableException, IllegalStateException, GooglePlayServicesNotAvailableException, IOException {
        zzb zzbVar = new zzb(context);
        boolean z = zzbVar.getBoolean("gads:ad_id_app_context:enabled", false);
        float f = zzbVar.getFloat("gads:ad_id_app_context:ping_ratio", 0.0f);
        String string = zzbVar.getString("gads:ad_id_use_shared_preference:experiment_id", "");
        AdvertisingIdClient advertisingIdClient = new AdvertisingIdClient(context, -1L, z, zzbVar.getBoolean("gads:ad_id_use_persistent_service:enabled", false));
        try {
            try {
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                advertisingIdClient.start(false);
                Info info = advertisingIdClient.getInfo();
                advertisingIdClient.zza(info, z, f, SystemClock.elapsedRealtime() - jElapsedRealtime, string, null);
                return info;
            } finally {
            }
        } finally {
            advertisingIdClient.finish();
        }
    }

    @Hide
    public static boolean getIsAdIdFakeForDebugLogging(Context context) throws GooglePlayServicesRepairableException, GooglePlayServicesNotAvailableException, IOException {
        zzb zzbVar = new zzb(context);
        AdvertisingIdClient advertisingIdClient = new AdvertisingIdClient(context, -1L, zzbVar.getBoolean("gads:ad_id_app_context:enabled", false), zzbVar.getBoolean("com.google.android.gms.ads.identifier.service.PERSISTENT_START", false));
        try {
            advertisingIdClient.start(false);
            return advertisingIdClient.getIsAdIdFakeForDebugLogging();
        } finally {
            advertisingIdClient.finish();
        }
    }

    @Hide
    public static void setShouldSkipGmsCoreVersionCheck(boolean z) {
    }

    @Hide
    private final void start(boolean z) throws GooglePlayServicesRepairableException, IllegalStateException, GooglePlayServicesNotAvailableException, IOException {
        zzbq.zzgw("Calling this from your main thread can lead to deadlock");
        synchronized (this) {
            if (this.zzamu) {
                finish();
            }
            this.zzams = zzc(this.mContext, this.zzamx);
            this.zzamt = zza(this.mContext, this.zzams);
            this.zzamu = true;
            if (z) {
                zzbm();
            }
        }
    }

    @Hide
    private static zzfp zza(Context context, com.google.android.gms.common.zza zzaVar) throws IOException {
        try {
            return zzfq.zzc(zzaVar.zza(10000L, TimeUnit.MILLISECONDS));
        } catch (InterruptedException unused) {
            throw new IOException("Interrupted exception");
        } catch (Throwable th) {
            throw new IOException(th);
        }
    }

    private final boolean zza(Info info, boolean z, float f, long j, String str, Throwable th) {
        if (Math.random() > f) {
            return false;
        }
        HashMap map = new HashMap();
        map.put("app_context", z ? "1" : "0");
        if (info != null) {
            map.put("limit_ad_tracking", info.isLimitAdTrackingEnabled() ? "1" : "0");
        }
        if (info != null && info.getId() != null) {
            map.put("ad_id_size", Integer.toString(info.getId().length()));
        }
        if (th != null) {
            map.put("error", th.getClass().getName());
        }
        if (str != null && !str.isEmpty()) {
            map.put("experiment_id", str);
        }
        map.put("tag", "AdvertisingIdClient");
        map.put("time_spent", Long.toString(j));
        new com.google.android.gms.ads.identifier.zza(this, map).start();
        return true;
    }

    private final void zzbm() {
        synchronized (this.zzamv) {
            if (this.zzamw != null) {
                this.zzamw.zzanc.countDown();
                try {
                    this.zzamw.join();
                } catch (InterruptedException unused) {
                }
            }
            if (this.zzamy > 0) {
                this.zzamw = new zza(this, this.zzamy);
            }
        }
    }

    private static com.google.android.gms.common.zza zzc(Context context, boolean z) throws GooglePlayServicesRepairableException, GooglePlayServicesNotAvailableException, IOException {
        try {
            context.getPackageManager().getPackageInfo("com.android.vending", 0);
            int iIsGooglePlayServicesAvailable = zzf.zzahf().isGooglePlayServicesAvailable(context);
            if (iIsGooglePlayServicesAvailable != 0 && iIsGooglePlayServicesAvailable != 2) {
                throw new IOException("Google Play services not available");
            }
            String str = z ? "com.google.android.gms.ads.identifier.service.PERSISTENT_START" : "com.google.android.gms.ads.identifier.service.START";
            com.google.android.gms.common.zza zzaVar = new com.google.android.gms.common.zza();
            Intent intent = new Intent(str);
            intent.setPackage("com.google.android.gms");
            try {
                if (com.google.android.gms.common.stats.zza.zzanm().zza(context, intent, zzaVar, 1)) {
                    return zzaVar;
                }
                throw new IOException("Connection failure");
            } catch (Throwable th) {
                throw new IOException(th);
            }
        } catch (PackageManager.NameNotFoundException unused) {
            throw new GooglePlayServicesNotAvailableException(9);
        }
    }

    @Hide
    protected void finalize() throws Throwable {
        finish();
        super.finalize();
    }

    @Hide
    public void finish() {
        zzbq.zzgw("Calling this from your main thread can lead to deadlock");
        synchronized (this) {
            if (this.mContext == null || this.zzams == null) {
                return;
            }
            try {
                if (this.zzamu) {
                    com.google.android.gms.common.stats.zza.zzanm();
                    this.mContext.unbindService(this.zzams);
                }
            } catch (Throwable th) {
                Log.i("AdvertisingIdClient", "AdvertisingIdClient unbindService failed.", th);
            }
            this.zzamu = false;
            this.zzamt = null;
            this.zzams = null;
        }
    }

    @Hide
    public Info getInfo() throws IOException {
        Info info;
        zzbq.zzgw("Calling this from your main thread can lead to deadlock");
        synchronized (this) {
            if (this.zzamu) {
                zzbq.checkNotNull(this.zzams);
                zzbq.checkNotNull(this.zzamt);
                info = new Info(this.zzamt.getId(), this.zzamt.zzb(true));
            } else {
                synchronized (this.zzamv) {
                    if (this.zzamw == null || !this.zzamw.zzand) {
                        throw new IOException("AdvertisingIdClient is not connected.");
                    }
                }
                try {
                    start(false);
                    if (!this.zzamu) {
                        throw new IOException("AdvertisingIdClient cannot reconnect.");
                    }
                    zzbq.checkNotNull(this.zzams);
                    zzbq.checkNotNull(this.zzamt);
                    try {
                        info = new Info(this.zzamt.getId(), this.zzamt.zzb(true));
                    } catch (RemoteException e) {
                        Log.i("AdvertisingIdClient", "GMS remote exception ", e);
                        throw new IOException("Remote exception");
                    }
                } catch (Exception e2) {
                    throw new IOException("AdvertisingIdClient cannot reconnect.", e2);
                }
            }
        }
        zzbm();
        return info;
    }

    @Hide
    public boolean getIsAdIdFakeForDebugLogging() throws IOException {
        boolean zZzbn;
        zzbq.zzgw("Calling this from your main thread can lead to deadlock");
        synchronized (this) {
            if (this.zzamu) {
                zzbq.checkNotNull(this.zzams);
                zzbq.checkNotNull(this.zzamt);
                zZzbn = this.zzamt.zzbn();
            } else {
                synchronized (this.zzamv) {
                    if (this.zzamw == null || !this.zzamw.zzand) {
                        throw new IOException("AdvertisingIdClient is not connected.");
                    }
                }
                try {
                    start(false);
                    if (!this.zzamu) {
                        throw new IOException("AdvertisingIdClient cannot reconnect.");
                    }
                    zzbq.checkNotNull(this.zzams);
                    zzbq.checkNotNull(this.zzamt);
                    try {
                        zZzbn = this.zzamt.zzbn();
                    } catch (RemoteException e) {
                        Log.i("AdvertisingIdClient", "GMS remote exception ", e);
                        throw new IOException("Remote exception");
                    }
                } catch (Exception e2) {
                    throw new IOException("AdvertisingIdClient cannot reconnect.", e2);
                }
            }
        }
        zzbm();
        return zZzbn;
    }

    @Hide
    public void start() throws GooglePlayServicesRepairableException, IllegalStateException, GooglePlayServicesNotAvailableException, IOException {
        start(true);
    }
}
