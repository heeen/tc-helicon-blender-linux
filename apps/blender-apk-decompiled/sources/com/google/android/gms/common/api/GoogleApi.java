package com.google.android.gms.common.api;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Api.ApiOptions;
import com.google.android.gms.common.api.internal.zzah;
import com.google.android.gms.common.api.internal.zzbm;
import com.google.android.gms.common.api.internal.zzbo;
import com.google.android.gms.common.api.internal.zzbw;
import com.google.android.gms.common.api.internal.zzci;
import com.google.android.gms.common.api.internal.zzck;
import com.google.android.gms.common.api.internal.zzcm;
import com.google.android.gms.common.api.internal.zzcq;
import com.google.android.gms.common.api.internal.zzcv;
import com.google.android.gms.common.api.internal.zzda;
import com.google.android.gms.common.api.internal.zzde;
import com.google.android.gms.common.api.internal.zzdo;
import com.google.android.gms.common.api.internal.zzh;
import com.google.android.gms.common.api.internal.zzm;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.common.internal.zzs;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import java.util.Collections;

/* JADX INFO: loaded from: classes.dex */
public class GoogleApi<O extends Api.ApiOptions> {
    private final Context mContext;
    private final int mId;
    private final Looper zzalj;
    private final Api<O> zzfop;
    private final O zzfsm;
    private final zzh<O> zzfsn;
    private final GoogleApiClient zzfso;
    private final zzda zzfsp;

    @Hide
    protected final zzbm zzfsq;

    @Hide
    public static class zza {
        public static final zza zzfsr = new zzd().zzahy();
        public final zzda zzfss;
        public final Looper zzfst;

        private zza(zzda zzdaVar, Account account, Looper looper) {
            this.zzfss = zzdaVar;
            this.zzfst = looper;
        }
    }

    @Hide
    @MainThread
    public GoogleApi(@NonNull Activity activity, Api<O> api, O o, zza zzaVar) {
        zzbq.checkNotNull(activity, "Null activity is not permitted.");
        zzbq.checkNotNull(api, "Api must not be null.");
        zzbq.checkNotNull(zzaVar, "Settings must not be null; use Settings.DEFAULT_SETTINGS instead.");
        this.mContext = activity.getApplicationContext();
        this.zzfop = api;
        this.zzfsm = o;
        this.zzalj = zzaVar.zzfst;
        this.zzfsn = zzh.zza(this.zzfop, this.zzfsm);
        this.zzfso = new zzbw(this);
        this.zzfsq = zzbm.zzck(this.mContext);
        this.mId = this.zzfsq.zzaka();
        this.zzfsp = zzaVar.zzfss;
        zzah.zza(activity, this.zzfsq, this.zzfsn);
        this.zzfsq.zza((GoogleApi<?>) this);
    }

    @Hide
    @Deprecated
    public GoogleApi(@NonNull Activity activity, Api<O> api, O o, zzda zzdaVar) {
        this(activity, (Api) api, (Api.ApiOptions) o, new zzd().zza(zzdaVar).zza(activity.getMainLooper()).zzahy());
    }

    @Hide
    protected GoogleApi(@NonNull Context context, Api<O> api, Looper looper) {
        zzbq.checkNotNull(context, "Null context is not permitted.");
        zzbq.checkNotNull(api, "Api must not be null.");
        zzbq.checkNotNull(looper, "Looper must not be null.");
        this.mContext = context.getApplicationContext();
        this.zzfop = api;
        this.zzfsm = null;
        this.zzalj = looper;
        this.zzfsn = zzh.zzb(api);
        this.zzfso = new zzbw(this);
        this.zzfsq = zzbm.zzck(this.mContext);
        this.mId = this.zzfsq.zzaka();
        this.zzfsp = new com.google.android.gms.common.api.internal.zzg();
    }

    @Hide
    @Deprecated
    public GoogleApi(@NonNull Context context, Api<O> api, O o, Looper looper, zzda zzdaVar) {
        this(context, api, (Api.ApiOptions) null, new zzd().zza(looper).zza(zzdaVar).zzahy());
    }

    @Hide
    public GoogleApi(@NonNull Context context, Api<O> api, O o, zza zzaVar) {
        zzbq.checkNotNull(context, "Null context is not permitted.");
        zzbq.checkNotNull(api, "Api must not be null.");
        zzbq.checkNotNull(zzaVar, "Settings must not be null; use Settings.DEFAULT_SETTINGS instead.");
        this.mContext = context.getApplicationContext();
        this.zzfop = api;
        this.zzfsm = o;
        this.zzalj = zzaVar.zzfst;
        this.zzfsn = zzh.zza(this.zzfop, this.zzfsm);
        this.zzfso = new zzbw(this);
        this.zzfsq = zzbm.zzck(this.mContext);
        this.mId = this.zzfsq.zzaka();
        this.zzfsp = zzaVar.zzfss;
        this.zzfsq.zza((GoogleApi<?>) this);
    }

    @Hide
    @Deprecated
    public GoogleApi(@NonNull Context context, Api<O> api, O o, zzda zzdaVar) {
        this(context, api, o, new zzd().zza(zzdaVar).zzahy());
    }

    private final <A extends Api.zzb, T extends zzm<? extends Result, A>> T zza(int i, @NonNull T t) {
        t.zzaiq();
        this.zzfsq.zza(this, i, (zzm<? extends Result, Api.zzb>) t);
        return t;
    }

    private final <TResult, A extends Api.zzb> Task<TResult> zza(int i, @NonNull zzde<A, TResult> zzdeVar) {
        TaskCompletionSource<TResult> taskCompletionSource = new TaskCompletionSource<>();
        this.zzfsq.zza(this, i, zzdeVar, taskCompletionSource, this.zzfsp);
        return taskCompletionSource.getTask();
    }

    @Hide
    private final zzs zzahx() {
        GoogleSignInAccount googleSignInAccount;
        GoogleSignInAccount googleSignInAccount2;
        return new zzs().zze((!(this.zzfsm instanceof Api.ApiOptions.HasGoogleSignInAccountOptions) || (googleSignInAccount2 = ((Api.ApiOptions.HasGoogleSignInAccountOptions) this.zzfsm).getGoogleSignInAccount()) == null) ? this.zzfsm instanceof Api.ApiOptions.HasAccountOptions ? ((Api.ApiOptions.HasAccountOptions) this.zzfsm).getAccount() : null : googleSignInAccount2.getAccount()).zze((!(this.zzfsm instanceof Api.ApiOptions.HasGoogleSignInAccountOptions) || (googleSignInAccount = ((Api.ApiOptions.HasGoogleSignInAccountOptions) this.zzfsm).getGoogleSignInAccount()) == null) ? Collections.emptySet() : googleSignInAccount.zzacf());
    }

    @Hide
    public final Context getApplicationContext() {
        return this.mContext;
    }

    @Hide
    public final int getInstanceId() {
        return this.mId;
    }

    @Hide
    public final Looper getLooper() {
        return this.zzalj;
    }

    @WorkerThread
    @Hide
    public Api.zze zza(Looper looper, zzbo<O> zzboVar) {
        return this.zzfop.zzahl().zza(this.mContext, looper, zzahx().zzgo(this.mContext.getPackageName()).zzgp(this.mContext.getClass().getName()).zzamn(), this.zzfsm, zzboVar, zzboVar);
    }

    @Hide
    public final <L> zzci<L> zza(@NonNull L l, String str) {
        return zzcm.zzb(l, this.zzalj, str);
    }

    @Hide
    public zzcv zza(Context context, Handler handler) {
        return new zzcv(context, handler, zzahx().zzamn());
    }

    @Hide
    public final <A extends Api.zzb, T extends zzm<? extends Result, A>> T zza(@NonNull T t) {
        return (T) zza(0, t);
    }

    @Hide
    public final Task<Boolean> zza(@NonNull zzck<?> zzckVar) {
        zzbq.checkNotNull(zzckVar, "Listener key cannot be null.");
        return this.zzfsq.zza(this, zzckVar);
    }

    @Hide
    public final <A extends Api.zzb, T extends zzcq<A, ?>, U extends zzdo<A, ?>> Task<Void> zza(@NonNull T t, U u) {
        zzbq.checkNotNull(t);
        zzbq.checkNotNull(u);
        zzbq.checkNotNull(t.zzakx(), "Listener has already been released.");
        zzbq.checkNotNull(u.zzakx(), "Listener has already been released.");
        zzbq.checkArgument(t.zzakx().equals(u.zzakx()), "Listener registration and unregistration methods must be constructed with the same ListenerHolder.");
        return this.zzfsq.zza(this, (zzcq<Api.zzb, ?>) t, (zzdo<Api.zzb, ?>) u);
    }

    @Hide
    public final <TResult, A extends Api.zzb> Task<TResult> zza(zzde<A, TResult> zzdeVar) {
        return zza(0, zzdeVar);
    }

    @Hide
    public final Api<O> zzaht() {
        return this.zzfop;
    }

    @Hide
    public final O zzahu() {
        return this.zzfsm;
    }

    @Hide
    public final zzh<O> zzahv() {
        return this.zzfsn;
    }

    @Hide
    public final GoogleApiClient zzahw() {
        return this.zzfso;
    }

    @Hide
    public final <A extends Api.zzb, T extends zzm<? extends Result, A>> T zzb(@NonNull T t) {
        return (T) zza(1, t);
    }

    @Hide
    public final <TResult, A extends Api.zzb> Task<TResult> zzb(zzde<A, TResult> zzdeVar) {
        return zza(1, zzdeVar);
    }

    @Hide
    public final <A extends Api.zzb, T extends zzm<? extends Result, A>> T zzc(@NonNull T t) {
        return (T) zza(2, t);
    }
}
