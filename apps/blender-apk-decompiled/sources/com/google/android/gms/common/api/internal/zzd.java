package com.google.android.gms.common.api.internal;

import android.os.RemoteException;
import android.support.annotation.NonNull;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: loaded from: classes.dex */
public final class zzd extends zzb<Void> {
    private zzcq<Api.zzb, ?> zzfty;
    private zzdo<Api.zzb, ?> zzftz;

    public zzd(zzcr zzcrVar, TaskCompletionSource<Void> taskCompletionSource) {
        super(3, taskCompletionSource);
        this.zzfty = zzcrVar.zzfty;
        this.zzftz = zzcrVar.zzftz;
    }

    @Override // com.google.android.gms.common.api.internal.zzb, com.google.android.gms.common.api.internal.zza
    public final /* bridge */ /* synthetic */ void zza(@NonNull zzae zzaeVar, boolean z) {
    }

    @Override // com.google.android.gms.common.api.internal.zzb
    public final /* bridge */ /* synthetic */ void zza(@NonNull RuntimeException runtimeException) {
        super.zza(runtimeException);
    }

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
    @Override // com.google.android.gms.common.api.internal.zzb
    public final void zzb(zzbo<?> zzboVar) throws RemoteException {
        this.zzfty.zzb(zzboVar.zzaix(), this.zzejm);
        if (this.zzfty.zzakx() != null) {
            zzboVar.zzakh().put(this.zzfty.zzakx(), new zzcr(this.zzfty, this.zzftz));
        }
    }

    @Override // com.google.android.gms.common.api.internal.zzb, com.google.android.gms.common.api.internal.zza
    public final /* bridge */ /* synthetic */ void zzs(@NonNull Status status) {
        super.zzs(status);
    }
}
