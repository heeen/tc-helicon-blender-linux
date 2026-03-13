package com.google.firebase;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.common.annotation.KeepForSdk;
import com.google.android.gms.common.api.internal.zzk;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbg;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.common.util.zzu;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.internal.InternalTokenProvider;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/* JADX INFO: loaded from: classes.dex */
public class FirebaseApp {
    public static final String DEFAULT_APP_NAME = "[DEFAULT]";
    private final Context mApplicationContext;
    private final String mName;
    private final FirebaseOptions zzmmr;
    private InternalTokenProvider zzmmx;
    private static final List<String> zzmmm = Arrays.asList("com.google.firebase.auth.FirebaseAuth", "com.google.firebase.iid.FirebaseInstanceId");
    private static final List<String> zzmmn = Collections.singletonList("com.google.firebase.crash.FirebaseCrash");
    private static final List<String> zzmmo = Arrays.asList("com.google.android.gms.measurement.AppMeasurement");
    private static final List<String> zzmmp = Arrays.asList(new String[0]);
    private static final Set<String> zzmmq = Collections.emptySet();
    private static final Object sLock = new Object();
    static final Map<String, FirebaseApp> zzimu = new ArrayMap();
    private final AtomicBoolean zzmms = new AtomicBoolean(false);
    private final AtomicBoolean zzmmt = new AtomicBoolean();
    private final List<IdTokenListener> zzmmu = new CopyOnWriteArrayList();
    private final List<zza> zzmmv = new CopyOnWriteArrayList();
    private final List<Object> zzmmw = new CopyOnWriteArrayList();
    private zzb zzmmy = new com.google.firebase.internal.zza();

    @Hide
    @KeepForSdk
    public interface IdTokenListener {
        void zzb(@NonNull com.google.firebase.internal.zzc zzcVar);
    }

    @Hide
    public interface zza {
        void zzbj(boolean z);
    }

    @Hide
    public interface zzb {
        void zzha(int i);
    }

    @Hide
    @TargetApi(24)
    static class zzc extends BroadcastReceiver {
        private static AtomicReference<zzc> zzmmz = new AtomicReference<>();
        private final Context mApplicationContext;

        private zzc(Context context) {
            this.mApplicationContext = context;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static void zzew(Context context) {
            if (zzmmz.get() == null) {
                zzc zzcVar = new zzc(context);
                if (zzmmz.compareAndSet(null, zzcVar)) {
                    context.registerReceiver(zzcVar, new IntentFilter("android.intent.action.USER_UNLOCKED"));
                }
            }
        }

        @Override // android.content.BroadcastReceiver
        public final void onReceive(Context context, Intent intent) {
            synchronized (FirebaseApp.sLock) {
                Iterator<FirebaseApp> it = FirebaseApp.zzimu.values().iterator();
                while (it.hasNext()) {
                    it.next().zzbsx();
                }
            }
            this.mApplicationContext.unregisterReceiver(this);
        }
    }

    @Hide
    private FirebaseApp(Context context, String str, FirebaseOptions firebaseOptions) {
        this.mApplicationContext = (Context) zzbq.checkNotNull(context);
        this.mName = zzbq.zzgv(str);
        this.zzmmr = (FirebaseOptions) zzbq.checkNotNull(firebaseOptions);
    }

    public static List<FirebaseApp> getApps(Context context) {
        ArrayList arrayList;
        com.google.firebase.internal.zzb.zzfb(context);
        synchronized (sLock) {
            arrayList = new ArrayList(zzimu.values());
            com.google.firebase.internal.zzb.zzclx();
            Set<String> setZzcly = com.google.firebase.internal.zzb.zzcly();
            setZzcly.removeAll(zzimu.keySet());
            for (String str : setZzcly) {
                com.google.firebase.internal.zzb.zzrw(str);
                arrayList.add(initializeApp(context, null, str));
            }
        }
        return arrayList;
    }

    @Nullable
    public static FirebaseApp getInstance() {
        FirebaseApp firebaseApp;
        synchronized (sLock) {
            firebaseApp = zzimu.get(DEFAULT_APP_NAME);
            if (firebaseApp == null) {
                String strZzany = zzu.zzany();
                StringBuilder sb = new StringBuilder(String.valueOf(strZzany).length() + 116);
                sb.append("Default FirebaseApp is not initialized in this process ");
                sb.append(strZzany);
                sb.append(". Make sure to call FirebaseApp.initializeApp(Context) first.");
                throw new IllegalStateException(sb.toString());
            }
        }
        return firebaseApp;
    }

    public static FirebaseApp getInstance(@NonNull String str) {
        FirebaseApp firebaseApp;
        String strConcat;
        synchronized (sLock) {
            firebaseApp = zzimu.get(str.trim());
            if (firebaseApp == null) {
                List<String> listZzbsw = zzbsw();
                if (listZzbsw.isEmpty()) {
                    strConcat = "";
                } else {
                    String strValueOf = String.valueOf(TextUtils.join(", ", listZzbsw));
                    strConcat = strValueOf.length() != 0 ? "Available app names: ".concat(strValueOf) : new String("Available app names: ");
                }
                throw new IllegalStateException(String.format("FirebaseApp with name %s doesn't exist. %s", str, strConcat));
            }
        }
        return firebaseApp;
    }

    @Nullable
    public static FirebaseApp initializeApp(Context context) {
        synchronized (sLock) {
            if (zzimu.containsKey(DEFAULT_APP_NAME)) {
                return getInstance();
            }
            FirebaseOptions firebaseOptionsFromResource = FirebaseOptions.fromResource(context);
            if (firebaseOptionsFromResource == null) {
                return null;
            }
            return initializeApp(context, firebaseOptionsFromResource);
        }
    }

    public static FirebaseApp initializeApp(Context context, FirebaseOptions firebaseOptions) {
        return initializeApp(context, firebaseOptions, DEFAULT_APP_NAME);
    }

    public static FirebaseApp initializeApp(Context context, FirebaseOptions firebaseOptions, String str) {
        FirebaseApp firebaseApp;
        com.google.firebase.internal.zzb.zzfb(context);
        if (context.getApplicationContext() instanceof Application) {
            zzk.zza((Application) context.getApplicationContext());
            zzk.zzaij().zza(new com.google.firebase.zza());
        }
        String strTrim = str.trim();
        if (context.getApplicationContext() != null) {
            context = context.getApplicationContext();
        }
        synchronized (sLock) {
            boolean z = !zzimu.containsKey(strTrim);
            StringBuilder sb = new StringBuilder(String.valueOf(strTrim).length() + 33);
            sb.append("FirebaseApp name ");
            sb.append(strTrim);
            sb.append(" already exists!");
            zzbq.zza(z, sb.toString());
            zzbq.checkNotNull(context, "Application context cannot be null.");
            firebaseApp = new FirebaseApp(context, strTrim, firebaseOptions);
            zzimu.put(strTrim, firebaseApp);
        }
        com.google.firebase.internal.zzb.zzg(firebaseApp);
        firebaseApp.zza(FirebaseApp.class, firebaseApp, zzmmm);
        if (firebaseApp.zzbsu()) {
            firebaseApp.zza(FirebaseApp.class, firebaseApp, zzmmn);
            firebaseApp.zza(Context.class, firebaseApp.getApplicationContext(), zzmmo);
        }
        return firebaseApp;
    }

    /* JADX WARN: Multi-variable type inference failed */
    private final <T> void zza(Class<T> cls, T t, Iterable<String> iterable) {
        boolean zIsDeviceProtectedStorage = ContextCompat.isDeviceProtectedStorage(this.mApplicationContext);
        if (zIsDeviceProtectedStorage) {
            zzc.zzew(this.mApplicationContext);
        }
        for (String str : iterable) {
            if (zIsDeviceProtectedStorage) {
                try {
                } catch (ClassNotFoundException unused) {
                    if (zzmmq.contains(str)) {
                        throw new IllegalStateException(String.valueOf(str).concat(" is missing, but is required. Check if it has been removed by Proguard."));
                    }
                    Log.d("FirebaseApp", String.valueOf(str).concat(" is not linked. Skipping initialization."));
                } catch (IllegalAccessException e) {
                    String strValueOf = String.valueOf(str);
                    Log.wtf("FirebaseApp", strValueOf.length() != 0 ? "Failed to initialize ".concat(strValueOf) : new String("Failed to initialize "), e);
                } catch (NoSuchMethodException unused2) {
                    throw new IllegalStateException(String.valueOf(str).concat("#getInstance has been removed by Proguard. Add keep rule to prevent it."));
                } catch (InvocationTargetException e2) {
                    Log.wtf("FirebaseApp", "Firebase API initialization failure.", e2);
                }
                if (zzmmp.contains(str)) {
                }
            }
            Method method = Class.forName(str).getMethod("getInstance", cls);
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
                method.invoke(null, t);
            }
        }
    }

    @Hide
    public static void zzbj(boolean z) {
        synchronized (sLock) {
            ArrayList arrayList = new ArrayList(zzimu.values());
            int size = arrayList.size();
            int i = 0;
            while (i < size) {
                Object obj = arrayList.get(i);
                i++;
                FirebaseApp firebaseApp = (FirebaseApp) obj;
                if (firebaseApp.zzmms.get()) {
                    firebaseApp.zzci(z);
                }
            }
        }
    }

    private final void zzbst() {
        zzbq.zza(!this.zzmmt.get(), "FirebaseApp was deleted");
    }

    private static List<String> zzbsw() {
        ArraySet arraySet = new ArraySet();
        synchronized (sLock) {
            Iterator<FirebaseApp> it = zzimu.values().iterator();
            while (it.hasNext()) {
                arraySet.add(it.next().getName());
            }
            if (com.google.firebase.internal.zzb.zzclx() != null) {
                arraySet.addAll(com.google.firebase.internal.zzb.zzcly());
            }
        }
        ArrayList arrayList = new ArrayList(arraySet);
        Collections.sort(arrayList);
        return arrayList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void zzbsx() {
        zza(FirebaseApp.class, this, zzmmm);
        if (zzbsu()) {
            zza(FirebaseApp.class, this, zzmmn);
            zza(Context.class, this.mApplicationContext, zzmmo);
        }
    }

    private final void zzci(boolean z) {
        Log.d("FirebaseApp", "Notifying background state change listeners.");
        Iterator<zza> it = this.zzmmv.iterator();
        while (it.hasNext()) {
            it.next().zzbj(z);
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof FirebaseApp) {
            return this.mName.equals(((FirebaseApp) obj).getName());
        }
        return false;
    }

    @NonNull
    public Context getApplicationContext() {
        zzbst();
        return this.mApplicationContext;
    }

    @NonNull
    public String getName() {
        zzbst();
        return this.mName;
    }

    @NonNull
    public FirebaseOptions getOptions() {
        zzbst();
        return this.zzmmr;
    }

    @Hide
    @KeepForSdk
    public Task<GetTokenResult> getToken(boolean z) {
        zzbst();
        return this.zzmmx == null ? Tasks.forException(new FirebaseApiNotAvailableException("firebase-auth is not linked, please fall back to unauthenticated mode.")) : this.zzmmx.zzcj(z);
    }

    @Hide
    @Nullable
    public final String getUid() throws FirebaseApiNotAvailableException {
        zzbst();
        if (this.zzmmx != null) {
            return this.zzmmx.getUid();
        }
        throw new FirebaseApiNotAvailableException("firebase-auth is not linked, please fall back to unauthenticated mode.");
    }

    public int hashCode() {
        return this.mName.hashCode();
    }

    public void setAutomaticResourceManagementEnabled(boolean z) {
        zzbst();
        if (this.zzmms.compareAndSet(!z, z)) {
            boolean zZzaik = zzk.zzaij().zzaik();
            if (z && zZzaik) {
                zzci(true);
            } else {
                if (z || !zZzaik) {
                    return;
                }
                zzci(false);
            }
        }
    }

    public String toString() {
        return zzbg.zzx(this).zzg("name", this.mName).zzg("options", this.zzmmr).toString();
    }

    @Hide
    public final void zza(@NonNull IdTokenListener idTokenListener) {
        zzbst();
        zzbq.checkNotNull(idTokenListener);
        this.zzmmu.add(idTokenListener);
        this.zzmmy.zzha(this.zzmmu.size());
    }

    @Hide
    public final void zza(zza zzaVar) {
        zzbst();
        if (this.zzmms.get() && zzk.zzaij().zzaik()) {
            zzaVar.zzbj(true);
        }
        this.zzmmv.add(zzaVar);
    }

    @Hide
    public final void zza(@NonNull zzb zzbVar) {
        this.zzmmy = (zzb) zzbq.checkNotNull(zzbVar);
        this.zzmmy.zzha(this.zzmmu.size());
    }

    @Hide
    public final void zza(@NonNull InternalTokenProvider internalTokenProvider) {
        this.zzmmx = (InternalTokenProvider) zzbq.checkNotNull(internalTokenProvider);
    }

    @UiThread
    @Hide
    public final void zza(@NonNull com.google.firebase.internal.zzc zzcVar) {
        Log.d("FirebaseApp", "Notifying auth state listeners.");
        Iterator<IdTokenListener> it = this.zzmmu.iterator();
        int i = 0;
        while (it.hasNext()) {
            it.next().zzb(zzcVar);
            i++;
        }
        Log.d("FirebaseApp", String.format("Notified %d auth state listeners.", Integer.valueOf(i)));
    }

    @Hide
    public final void zzb(@NonNull IdTokenListener idTokenListener) {
        zzbst();
        zzbq.checkNotNull(idTokenListener);
        this.zzmmu.remove(idTokenListener);
        this.zzmmy.zzha(this.zzmmu.size());
    }

    @Hide
    public final boolean zzbsu() {
        return DEFAULT_APP_NAME.equals(getName());
    }

    @Hide
    public final String zzbsv() {
        String strZzl = com.google.android.gms.common.util.zzc.zzl(getName().getBytes());
        String strZzl2 = com.google.android.gms.common.util.zzc.zzl(getOptions().getApplicationId().getBytes());
        StringBuilder sb = new StringBuilder(String.valueOf(strZzl).length() + 1 + String.valueOf(strZzl2).length());
        sb.append(strZzl);
        sb.append("+");
        sb.append(strZzl2);
        return sb.toString();
    }
}
