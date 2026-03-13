package com.google.android.gms.common.api.internal;

import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.AvailabilityException;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzj {
    private int zzfum;
    private final ArrayMap<zzh<?>, String> zzfuk = new ArrayMap<>();
    private final TaskCompletionSource<Map<zzh<?>, String>> zzful = new TaskCompletionSource<>();
    private boolean zzfun = false;
    private final ArrayMap<zzh<?>, ConnectionResult> zzfse = new ArrayMap<>();

    /* JADX WARN: Type inference fix 'apply assigned field type' failed
    java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$UnknownArg
    	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:593)
    	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
    	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
     */
    public zzj(Iterable<? extends GoogleApi<?>> iterable) {
        Iterator<? extends GoogleApi<?>> it = iterable.iterator();
        while (it.hasNext()) {
            this.zzfse.put(it.next().zzahv(), null);
        }
        this.zzfum = this.zzfse.keySet().size();
    }

    public final Task<Map<zzh<?>, String>> getTask() {
        return this.zzful.getTask();
    }

    public final void zza(zzh<?> zzhVar, ConnectionResult connectionResult, @Nullable String str) {
        this.zzfse.put(zzhVar, connectionResult);
        this.zzfuk.put(zzhVar, str);
        this.zzfum--;
        if (!connectionResult.isSuccess()) {
            this.zzfun = true;
        }
        if (this.zzfum == 0) {
            if (!this.zzfun) {
                this.zzful.setResult(this.zzfuk);
            } else {
                this.zzful.setException(new AvailabilityException(this.zzfse));
            }
        }
    }

    public final Set<zzh<?>> zzaii() {
        return this.zzfse.keySet();
    }
}
