package com.google.android.gms.common.api;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.widget.ActivityChooserView;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Api.ApiOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzan;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.common.internal.zzj;
import com.google.android.gms.common.internal.zzp;
import com.google.android.gms.common.internal.zzr;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class Api<O extends ApiOptions> {
    private final String mName;
    private final zza<?, O> zzfsa;
    private final zzh<?, O> zzfsb;
    private final zzf<?> zzfsc;
    private final zzi<?> zzfsd;

    public interface ApiOptions {

        public interface HasAccountOptions extends HasOptions, NotRequiredOptions {
            Account getAccount();
        }

        public interface HasGoogleSignInAccountOptions extends HasOptions {
            GoogleSignInAccount getGoogleSignInAccount();
        }

        public interface HasOptions extends ApiOptions {
        }

        public static final class NoOptions implements NotRequiredOptions {
            private NoOptions() {
            }
        }

        public interface NotRequiredOptions extends ApiOptions {
        }

        public interface Optional extends HasOptions, NotRequiredOptions {
        }
    }

    @Hide
    public static abstract class zza<T extends zze, O> extends zzd<T, O> {
        @Hide
        public abstract T zza(Context context, Looper looper, zzr zzrVar, O o, GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener);
    }

    @Hide
    public interface zzb {
    }

    @Hide
    public static class zzc<C extends zzb> {
    }

    @Hide
    public static abstract class zzd<T extends zzb, O> {
        public int getPriority() {
            return ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        }

        public List<Scope> zzr(O o) {
            return Collections.emptyList();
        }
    }

    @Hide
    public interface zze extends zzb {
        void disconnect();

        void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

        Intent getSignInIntent();

        boolean isConnected();

        boolean isConnecting();

        void zza(zzan zzanVar, Set<Scope> set);

        void zza(zzj zzjVar);

        void zza(zzp zzpVar);

        boolean zzacc();

        boolean zzacn();

        boolean zzahn();

        @Nullable
        IBinder zzaho();

        @Hide
        String zzahp();

        @Hide
        int zzahq();
    }

    @Hide
    public static final class zzf<C extends zze> extends zzc<C> {
    }

    @Hide
    public interface zzg<T extends IInterface> extends zzb {
    }

    @Hide
    public static abstract class zzh<T extends zzg, O> extends zzd<T, O> {
    }

    @Hide
    public static final class zzi<C extends zzg> extends zzc<C> {
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Hide
    public <C extends zze> Api(String str, zza<C, O> zzaVar, zzf<C> zzfVar) {
        zzbq.checkNotNull(zzaVar, "Cannot construct an Api with a null ClientBuilder");
        zzbq.checkNotNull(zzfVar, "Cannot construct an Api with a null ClientKey");
        this.mName = str;
        this.zzfsa = zzaVar;
        this.zzfsb = null;
        this.zzfsc = zzfVar;
        this.zzfsd = null;
    }

    @Hide
    public final String getName() {
        return this.mName;
    }

    @Hide
    public final zzd<?, O> zzahk() {
        return this.zzfsa;
    }

    @Hide
    public final zza<?, O> zzahl() {
        zzbq.zza(this.zzfsa != null, "This API was constructed with a SimpleClientBuilder. Use getSimpleClientBuilder");
        return this.zzfsa;
    }

    @Hide
    public final zzc<?> zzahm() {
        if (this.zzfsc != null) {
            return this.zzfsc;
        }
        throw new IllegalStateException("This API was constructed with null client keys. This should not be possible.");
    }
}
