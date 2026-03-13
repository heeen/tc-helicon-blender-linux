package com.google.firebase.iid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.firebase.FirebaseApp;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public class FirebaseInstanceId {
    private static zzaa zzoko;
    private static ScheduledThreadPoolExecutor zzokp;
    private KeyPair zzimx;
    private final FirebaseApp zzmwq;
    private final zzw zzokq;
    private final zzx zzokr;
    private boolean zzoks = false;
    private boolean zzokt;
    private static final long zzokn = TimeUnit.HOURS.toSeconds(8);
    private static Map<String, FirebaseInstanceId> zzimu = new ArrayMap();

    private FirebaseInstanceId(FirebaseApp firebaseApp, zzw zzwVar) {
        if (zzw.zzf(firebaseApp) == null) {
            throw new IllegalStateException("FirebaseInstanceId failed to initialize, FirebaseApp is missing project ID");
        }
        this.zzmwq = firebaseApp;
        this.zzokq = zzwVar;
        this.zzokr = new zzx(firebaseApp.getApplicationContext(), zzwVar);
        this.zzokt = zzcli();
        if (zzclk()) {
            zzclb();
        }
    }

    public static FirebaseInstanceId getInstance() {
        return getInstance(FirebaseApp.getInstance());
    }

    @Keep
    public static synchronized FirebaseInstanceId getInstance(@NonNull FirebaseApp firebaseApp) {
        FirebaseInstanceId firebaseInstanceId;
        firebaseInstanceId = zzimu.get(firebaseApp.getOptions().getApplicationId());
        if (firebaseInstanceId == null) {
            if (zzoko == null) {
                zzoko = new zzaa(firebaseApp.getApplicationContext());
            }
            firebaseInstanceId = new FirebaseInstanceId(firebaseApp, new zzw(firebaseApp.getApplicationContext()));
            zzimu.put(firebaseApp.getOptions().getApplicationId(), firebaseInstanceId);
        }
        return firebaseInstanceId;
    }

    private final synchronized void startSync() {
        if (!this.zzoks) {
            zzcd(0L);
        }
    }

    private final synchronized KeyPair zzawp() {
        if (this.zzimx == null) {
            this.zzimx = zzoko.zzrs("");
        }
        if (this.zzimx == null) {
            this.zzimx = zzoko.zzrq("");
        }
        return this.zzimx;
    }

    private final String zzb(String str, String str2, Bundle bundle) throws IOException {
        bundle.putString("scope", str2);
        bundle.putString("sender", str);
        bundle.putString("subtype", str);
        bundle.putString("appid", getId());
        bundle.putString("gmp_app_id", this.zzmwq.getOptions().getApplicationId());
        bundle.putString("gmsv", Integer.toString(this.zzokq.zzclo()));
        bundle.putString("osv", Integer.toString(Build.VERSION.SDK_INT));
        bundle.putString("app_ver", this.zzokq.zzclm());
        bundle.putString("app_ver_name", this.zzokq.zzcln());
        bundle.putString("cliv", "fiid-12211000");
        Bundle bundleZzah = this.zzokr.zzah(bundle);
        if (bundleZzah == null) {
            throw new IOException("SERVICE_NOT_AVAILABLE");
        }
        String string = bundleZzah.getString("registration_id");
        if (string != null) {
            return string;
        }
        String string2 = bundleZzah.getString("unregistered");
        if (string2 != null) {
            return string2;
        }
        String string3 = bundleZzah.getString("error");
        if ("RST".equals(string3)) {
            zzclg();
            throw new IOException("INSTANCE_ID_RESET");
        }
        if (string3 != null) {
            throw new IOException(string3);
        }
        String strValueOf = String.valueOf(bundleZzah);
        StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 21);
        sb.append("Unexpected response: ");
        sb.append(strValueOf);
        Log.w("FirebaseInstanceId", sb.toString(), new Throwable());
        throw new IOException("SERVICE_NOT_AVAILABLE");
    }

    static void zzb(Runnable runnable, long j) {
        synchronized (FirebaseInstanceId.class) {
            if (zzokp == null) {
                zzokp = new ScheduledThreadPoolExecutor(1);
            }
            zzokp.schedule(runnable, j, TimeUnit.SECONDS);
        }
    }

    private final void zzclb() {
        zzab zzabVarZzclc = zzclc();
        if (zzabVarZzclc == null || zzabVarZzclc.zzru(this.zzokq.zzclm()) || zzoko.zzcls() != null) {
            startSync();
        }
    }

    static zzaa zzcle() {
        return zzoko;
    }

    static boolean zzclf() {
        if (Log.isLoggable("FirebaseInstanceId", 3)) {
            return true;
        }
        return Build.VERSION.SDK_INT == 23 && Log.isLoggable("FirebaseInstanceId", 3);
    }

    private final boolean zzcli() {
        ApplicationInfo applicationInfo;
        Context applicationContext = this.zzmwq.getApplicationContext();
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences("com.google.firebase.messaging", 0);
        if (sharedPreferences.contains("auto_init")) {
            return sharedPreferences.getBoolean("auto_init", true);
        }
        try {
            PackageManager packageManager = applicationContext.getPackageManager();
            if (packageManager != null && (applicationInfo = packageManager.getApplicationInfo(applicationContext.getPackageName(), 128)) != null && applicationInfo.metaData != null && applicationInfo.metaData.containsKey("firebase_messaging_auto_init_enabled")) {
                return applicationInfo.metaData.getBoolean("firebase_messaging_auto_init_enabled");
            }
        } catch (PackageManager.NameNotFoundException unused) {
        }
        return zzclj();
    }

    private final boolean zzclj() {
        try {
            Class.forName("com.google.firebase.messaging.FirebaseMessaging");
            return true;
        } catch (ClassNotFoundException unused) {
            Context applicationContext = this.zzmwq.getApplicationContext();
            Intent intent = new Intent("com.google.firebase.MESSAGING_EVENT");
            intent.setPackage(applicationContext.getPackageName());
            ResolveInfo resolveInfoResolveService = applicationContext.getPackageManager().resolveService(intent, 0);
            return (resolveInfoResolveService == null || resolveInfoResolveService.serviceInfo == null) ? false : true;
        }
    }

    @WorkerThread
    public void deleteInstanceId() throws IOException {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IOException("MAIN_THREAD");
        }
        Bundle bundle = new Bundle();
        bundle.putString("iid-operation", "delete");
        bundle.putString("delete", "1");
        zzb("*", "*", bundle);
        zzclg();
    }

    @WorkerThread
    public void deleteToken(String str, String str2) throws IOException {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IOException("MAIN_THREAD");
        }
        Bundle bundle = new Bundle();
        bundle.putString("delete", "1");
        zzb(str, str2, bundle);
        zzoko.zzg("", str, str2);
    }

    final FirebaseApp getApp() {
        return this.zzmwq;
    }

    public long getCreationTime() {
        return zzoko.zzrp("");
    }

    @WorkerThread
    public String getId() {
        zzclb();
        return zzw.zzb(zzawp());
    }

    @Nullable
    public String getToken() {
        zzab zzabVarZzclc = zzclc();
        if (zzabVarZzclc == null || zzabVarZzclc.zzru(this.zzokq.zzclm())) {
            startSync();
        }
        if (zzabVarZzclc != null) {
            return zzabVarZzclc.zzlnm;
        }
        return null;
    }

    @WorkerThread
    public String getToken(String str, String str2) throws IOException {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IOException("MAIN_THREAD");
        }
        zzab zzabVarZzq = zzoko.zzq("", str, str2);
        if (zzabVarZzq != null && !zzabVarZzq.zzru(this.zzokq.zzclm())) {
            return zzabVarZzq.zzlnm;
        }
        String strZzb = zzb(str, str2, new Bundle());
        if (strZzb != null) {
            zzoko.zza("", str, str2, strZzb, this.zzokq.zzclm());
        }
        return strZzb;
    }

    final synchronized void zzcd(long j) {
        zzb(new zzac(this, this.zzokq, Math.min(Math.max(30L, j << 1), zzokn)), j);
        this.zzoks = true;
    }

    @Nullable
    final zzab zzclc() {
        return zzoko.zzq("", zzw.zzf(this.zzmwq), "*");
    }

    @Hide
    final String zzcld() throws IOException {
        return getToken(zzw.zzf(this.zzmwq), "*");
    }

    final synchronized void zzclg() {
        zzoko.zzawz();
        this.zzimx = null;
        if (zzclk()) {
            startSync();
        }
    }

    final void zzclh() {
        zzoko.zzrr("");
        startSync();
    }

    @Hide
    public final synchronized boolean zzclk() {
        return this.zzokt;
    }

    final synchronized void zzcy(boolean z) {
        this.zzoks = z;
    }

    @Hide
    public final synchronized void zzcz(boolean z) {
        SharedPreferences.Editor editorEdit = this.zzmwq.getApplicationContext().getSharedPreferences("com.google.firebase.messaging", 0).edit();
        editorEdit.putBoolean("auto_init", z);
        editorEdit.apply();
        if (!this.zzokt && z) {
            zzclb();
        }
        this.zzokt = z;
    }

    @Hide
    public final synchronized void zzrl(String str) {
        zzoko.zzrl(str);
        startSync();
    }

    final void zzrm(String str) throws IOException {
        zzab zzabVarZzclc = zzclc();
        if (zzabVarZzclc == null || zzabVarZzclc.zzru(this.zzokq.zzclm())) {
            throw new IOException("token not available");
        }
        Bundle bundle = new Bundle();
        String strValueOf = String.valueOf("/topics/");
        String strValueOf2 = String.valueOf(str);
        bundle.putString("gcm.topic", strValueOf2.length() != 0 ? strValueOf.concat(strValueOf2) : new String(strValueOf));
        String str2 = zzabVarZzclc.zzlnm;
        String strValueOf3 = String.valueOf("/topics/");
        String strValueOf4 = String.valueOf(str);
        zzb(str2, strValueOf4.length() != 0 ? strValueOf3.concat(strValueOf4) : new String(strValueOf3), bundle);
    }

    final void zzrn(String str) throws IOException {
        zzab zzabVarZzclc = zzclc();
        if (zzabVarZzclc == null || zzabVarZzclc.zzru(this.zzokq.zzclm())) {
            throw new IOException("token not available");
        }
        Bundle bundle = new Bundle();
        String strValueOf = String.valueOf("/topics/");
        String strValueOf2 = String.valueOf(str);
        bundle.putString("gcm.topic", strValueOf2.length() != 0 ? strValueOf.concat(strValueOf2) : new String(strValueOf));
        bundle.putString("delete", "1");
        String str2 = zzabVarZzclc.zzlnm;
        String strValueOf3 = String.valueOf("/topics/");
        String strValueOf4 = String.valueOf(str);
        zzb(str2, strValueOf4.length() != 0 ? strValueOf3.concat(strValueOf4) : new String(strValueOf3), bundle);
    }
}
