package com.google.android.gms.common.api;

import android.accounts.Account;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.ArrayMap;
import android.view.View;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.internal.zzba;
import com.google.android.gms.common.api.internal.zzce;
import com.google.android.gms.common.api.internal.zzci;
import com.google.android.gms.common.api.internal.zzcu;
import com.google.android.gms.common.api.internal.zzdh;
import com.google.android.gms.common.api.internal.zzi;
import com.google.android.gms.common.api.internal.zzm;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.common.internal.zzr;
import com.google.android.gms.common.internal.zzt;
import com.google.android.gms.internal.zzcyg;
import com.google.android.gms.internal.zzcyj;
import com.google.android.gms.internal.zzcyk;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/* JADX INFO: loaded from: classes.dex */
public abstract class GoogleApiClient {
    public static final int SIGN_IN_MODE_OPTIONAL = 2;
    public static final int SIGN_IN_MODE_REQUIRED = 1;
    private static final Set<GoogleApiClient> zzfsv = Collections.newSetFromMap(new WeakHashMap());

    public static final class Builder {
        private final Context mContext;
        private Looper zzalj;
        private String zzehh;
        private Account zzeho;
        private final Set<Scope> zzfsw;
        private final Set<Scope> zzfsx;
        private int zzfsy;
        private View zzfsz;
        private String zzfta;
        private final Map<Api<?>, zzt> zzftb;
        private final Map<Api<?>, Api.ApiOptions> zzftc;
        private zzce zzftd;
        private int zzfte;
        private OnConnectionFailedListener zzftf;
        private GoogleApiAvailability zzftg;
        private Api.zza<? extends zzcyj, zzcyk> zzfth;
        private final ArrayList<ConnectionCallbacks> zzfti;
        private final ArrayList<OnConnectionFailedListener> zzftj;
        private boolean zzftk;

        public Builder(@NonNull Context context) {
            this.zzfsw = new HashSet();
            this.zzfsx = new HashSet();
            this.zzftb = new ArrayMap();
            this.zzftc = new ArrayMap();
            this.zzfte = -1;
            this.zzftg = GoogleApiAvailability.getInstance();
            this.zzfth = zzcyg.zzegv;
            this.zzfti = new ArrayList<>();
            this.zzftj = new ArrayList<>();
            this.zzftk = false;
            this.mContext = context;
            this.zzalj = context.getMainLooper();
            this.zzehh = context.getPackageName();
            this.zzfta = context.getClass().getName();
        }

        public Builder(@NonNull Context context, @NonNull ConnectionCallbacks connectionCallbacks, @NonNull OnConnectionFailedListener onConnectionFailedListener) {
            this(context);
            zzbq.checkNotNull(connectionCallbacks, "Must provide a connected listener");
            this.zzfti.add(connectionCallbacks);
            zzbq.checkNotNull(onConnectionFailedListener, "Must provide a connection failed listener");
            this.zzftj.add(onConnectionFailedListener);
        }

        private final <O extends Api.ApiOptions> void zza(Api<O> api, O o, Scope... scopeArr) {
            HashSet hashSet = new HashSet(api.zzahk().zzr(o));
            for (Scope scope : scopeArr) {
                hashSet.add(scope);
            }
            this.zzftb.put(api, new zzt(hashSet));
        }

        public final Builder addApi(@NonNull Api<? extends Api.ApiOptions.NotRequiredOptions> api) {
            zzbq.checkNotNull(api, "Api must not be null");
            this.zzftc.put(api, null);
            List<Scope> listZzr = api.zzahk().zzr(null);
            this.zzfsx.addAll(listZzr);
            this.zzfsw.addAll(listZzr);
            return this;
        }

        public final <O extends Api.ApiOptions.HasOptions> Builder addApi(@NonNull Api<O> api, @NonNull O o) {
            zzbq.checkNotNull(api, "Api must not be null");
            zzbq.checkNotNull(o, "Null options are not permitted for this Api");
            this.zzftc.put(api, o);
            List<Scope> listZzr = api.zzahk().zzr(o);
            this.zzfsx.addAll(listZzr);
            this.zzfsw.addAll(listZzr);
            return this;
        }

        public final <O extends Api.ApiOptions.HasOptions> Builder addApiIfAvailable(@NonNull Api<O> api, @NonNull O o, Scope... scopeArr) {
            zzbq.checkNotNull(api, "Api must not be null");
            zzbq.checkNotNull(o, "Null options are not permitted for this Api");
            this.zzftc.put(api, o);
            zza(api, o, scopeArr);
            return this;
        }

        public final Builder addApiIfAvailable(@NonNull Api<? extends Api.ApiOptions.NotRequiredOptions> api, Scope... scopeArr) {
            zzbq.checkNotNull(api, "Api must not be null");
            this.zzftc.put(api, null);
            zza(api, null, scopeArr);
            return this;
        }

        public final Builder addConnectionCallbacks(@NonNull ConnectionCallbacks connectionCallbacks) {
            zzbq.checkNotNull(connectionCallbacks, "Listener must not be null");
            this.zzfti.add(connectionCallbacks);
            return this;
        }

        public final Builder addOnConnectionFailedListener(@NonNull OnConnectionFailedListener onConnectionFailedListener) {
            zzbq.checkNotNull(onConnectionFailedListener, "Listener must not be null");
            this.zzftj.add(onConnectionFailedListener);
            return this;
        }

        public final Builder addScope(@NonNull Scope scope) {
            zzbq.checkNotNull(scope, "Scope must not be null");
            this.zzfsw.add(scope);
            return this;
        }

        public final GoogleApiClient build() {
            zzbq.checkArgument(!this.zzftc.isEmpty(), "must call addApi() to add at least one API");
            zzr zzrVarZzaic = zzaic();
            Api<?> api = null;
            Map<Api<?>, zzt> mapZzamh = zzrVarZzaic.zzamh();
            ArrayMap arrayMap = new ArrayMap();
            ArrayMap arrayMap2 = new ArrayMap();
            ArrayList arrayList = new ArrayList();
            boolean z = false;
            for (Api<?> api2 : this.zzftc.keySet()) {
                Api.ApiOptions apiOptions = this.zzftc.get(api2);
                boolean z2 = mapZzamh.get(api2) != null;
                arrayMap.put(api2, Boolean.valueOf(z2));
                com.google.android.gms.common.api.internal.zzt zztVar = new com.google.android.gms.common.api.internal.zzt(api2, z2);
                arrayList.add(zztVar);
                Api.zza<?, O> zzaVarZzahl = api2.zzahl();
                Api.zze zzeVarZza = zzaVarZzahl.zza(this.mContext, this.zzalj, zzrVarZzaic, apiOptions, zztVar, zztVar);
                arrayMap2.put(api2.zzahm(), zzeVarZza);
                if (zzaVarZzahl.getPriority() == 1) {
                    z = apiOptions != null;
                }
                if (zzeVarZza.zzacn()) {
                    if (api != null) {
                        String name = api2.getName();
                        String name2 = api.getName();
                        StringBuilder sb = new StringBuilder(String.valueOf(name).length() + 21 + String.valueOf(name2).length());
                        sb.append(name);
                        sb.append(" cannot be used with ");
                        sb.append(name2);
                        throw new IllegalStateException(sb.toString());
                    }
                    api = api2;
                }
            }
            if (api != null) {
                if (z) {
                    String name3 = api.getName();
                    StringBuilder sb2 = new StringBuilder(String.valueOf(name3).length() + 82);
                    sb2.append("With using ");
                    sb2.append(name3);
                    sb2.append(", GamesOptions can only be specified within GoogleSignInOptions.Builder");
                    throw new IllegalStateException(sb2.toString());
                }
                zzbq.zza(this.zzeho == null, "Must not set an account in GoogleApiClient.Builder when using %s. Set account in GoogleSignInOptions.Builder instead", api.getName());
                zzbq.zza(this.zzfsw.equals(this.zzfsx), "Must not set scopes in GoogleApiClient.Builder when using %s. Set account in GoogleSignInOptions.Builder instead.", api.getName());
            }
            zzba zzbaVar = new zzba(this.mContext, new ReentrantLock(), this.zzalj, zzrVarZzaic, this.zzftg, this.zzfth, arrayMap, this.zzfti, this.zzftj, arrayMap2, this.zzfte, zzba.zza(arrayMap2.values(), true), arrayList, false);
            synchronized (GoogleApiClient.zzfsv) {
                GoogleApiClient.zzfsv.add(zzbaVar);
            }
            if (this.zzfte >= 0) {
                zzi.zza(this.zzftd).zza(this.zzfte, zzbaVar, this.zzftf);
            }
            return zzbaVar;
        }

        public final Builder enableAutoManage(@NonNull FragmentActivity fragmentActivity, int i, @Nullable OnConnectionFailedListener onConnectionFailedListener) {
            zzce zzceVar = new zzce(fragmentActivity);
            zzbq.checkArgument(i >= 0, "clientId must be non-negative");
            this.zzfte = i;
            this.zzftf = onConnectionFailedListener;
            this.zzftd = zzceVar;
            return this;
        }

        public final Builder enableAutoManage(@NonNull FragmentActivity fragmentActivity, @Nullable OnConnectionFailedListener onConnectionFailedListener) {
            return enableAutoManage(fragmentActivity, 0, onConnectionFailedListener);
        }

        public final Builder setAccountName(String str) {
            this.zzeho = str == null ? null : new Account(str, "com.google");
            return this;
        }

        public final Builder setGravityForPopups(int i) {
            this.zzfsy = i;
            return this;
        }

        public final Builder setHandler(@NonNull Handler handler) {
            zzbq.checkNotNull(handler, "Handler must not be null");
            this.zzalj = handler.getLooper();
            return this;
        }

        public final Builder setViewForPopups(@NonNull View view) {
            zzbq.checkNotNull(view, "View must not be null");
            this.zzfsz = view;
            return this;
        }

        public final Builder useDefaultAccount() {
            return setAccountName("<<default account>>");
        }

        @Hide
        public final zzr zzaic() {
            zzcyk zzcykVar = zzcyk.zzklp;
            if (this.zzftc.containsKey(zzcyg.API)) {
                zzcykVar = (zzcyk) this.zzftc.get(zzcyg.API);
            }
            return new zzr(this.zzeho, this.zzfsw, this.zzftb, this.zzfsy, this.zzfsz, this.zzehh, this.zzfta, zzcykVar);
        }
    }

    public interface ConnectionCallbacks {
        public static final int CAUSE_NETWORK_LOST = 2;
        public static final int CAUSE_SERVICE_DISCONNECTED = 1;

        void onConnected(@Nullable Bundle bundle);

        void onConnectionSuspended(int i);
    }

    public interface OnConnectionFailedListener {
        void onConnectionFailed(@NonNull ConnectionResult connectionResult);
    }

    public static void dumpAll(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (zzfsv) {
            int i = 0;
            String strConcat = String.valueOf(str).concat("  ");
            for (GoogleApiClient googleApiClient : zzfsv) {
                printWriter.append((CharSequence) str).append("GoogleApiClient#").println(i);
                googleApiClient.dump(strConcat, fileDescriptor, printWriter, strArr);
                i++;
            }
        }
    }

    @Hide
    public static Set<GoogleApiClient> zzahz() {
        Set<GoogleApiClient> set;
        synchronized (zzfsv) {
            set = zzfsv;
        }
        return set;
    }

    public abstract ConnectionResult blockingConnect();

    public abstract ConnectionResult blockingConnect(long j, @NonNull TimeUnit timeUnit);

    public abstract PendingResult<Status> clearDefaultAccountAndReconnect();

    public abstract void connect();

    public void connect(int i) {
        throw new UnsupportedOperationException();
    }

    public abstract void disconnect();

    public abstract void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

    @NonNull
    public abstract ConnectionResult getConnectionResult(@NonNull Api<?> api);

    @Hide
    public Context getContext() {
        throw new UnsupportedOperationException();
    }

    @Hide
    public Looper getLooper() {
        throw new UnsupportedOperationException();
    }

    public abstract boolean hasConnectedApi(@NonNull Api<?> api);

    public abstract boolean isConnected();

    public abstract boolean isConnecting();

    public abstract boolean isConnectionCallbacksRegistered(@NonNull ConnectionCallbacks connectionCallbacks);

    public abstract boolean isConnectionFailedListenerRegistered(@NonNull OnConnectionFailedListener onConnectionFailedListener);

    public abstract void reconnect();

    public abstract void registerConnectionCallbacks(@NonNull ConnectionCallbacks connectionCallbacks);

    public abstract void registerConnectionFailedListener(@NonNull OnConnectionFailedListener onConnectionFailedListener);

    public abstract void stopAutoManage(@NonNull FragmentActivity fragmentActivity);

    public abstract void unregisterConnectionCallbacks(@NonNull ConnectionCallbacks connectionCallbacks);

    public abstract void unregisterConnectionFailedListener(@NonNull OnConnectionFailedListener onConnectionFailedListener);

    @Hide
    @NonNull
    public <C extends Api.zze> C zza(@NonNull Api.zzc<C> zzcVar) {
        throw new UnsupportedOperationException();
    }

    @Hide
    public void zza(zzdh zzdhVar) {
        throw new UnsupportedOperationException();
    }

    @Hide
    public boolean zza(@NonNull Api<?> api) {
        throw new UnsupportedOperationException();
    }

    @Hide
    public boolean zza(zzcu zzcuVar) {
        throw new UnsupportedOperationException();
    }

    @Hide
    public void zzaia() {
        throw new UnsupportedOperationException();
    }

    @Hide
    public void zzb(zzdh zzdhVar) {
        throw new UnsupportedOperationException();
    }

    @Hide
    public <A extends Api.zzb, R extends Result, T extends zzm<R, A>> T zzd(@NonNull T t) {
        throw new UnsupportedOperationException();
    }

    @Hide
    public <A extends Api.zzb, T extends zzm<? extends Result, A>> T zze(@NonNull T t) {
        throw new UnsupportedOperationException();
    }

    @Hide
    public <L> zzci<L> zzt(@NonNull L l) {
        throw new UnsupportedOperationException();
    }
}
