package com.google.android.gms.common.api.internal;

import android.support.annotation.NonNull;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.TaskCompletionSource;

/* JADX INFO: loaded from: classes.dex */
public final class zzf extends zzb<Boolean> {
    private zzck<?> zzfuc;

    public zzf(zzck<?> zzckVar, TaskCompletionSource<Boolean> taskCompletionSource) {
        super(4, taskCompletionSource);
        this.zzfuc = zzckVar;
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
    /*  JADX ERROR: JadxRuntimeException in pass: FinishTypeInference
        jadx.core.utils.exceptions.JadxRuntimeException: Code variable not set in r3v4 boolean
        	at jadx.core.dex.instructions.args.SSAVar.getCodeVar(SSAVar.java:236)
        	at jadx.core.dex.visitors.typeinference.FinishTypeInference.lambda$visit$0(FinishTypeInference.java:27)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
        	at jadx.core.dex.visitors.typeinference.FinishTypeInference.visit(FinishTypeInference.java:22)
        */
    @Override // com.google.android.gms.common.api.internal.zzb
    public final void zzb(com.google.android.gms.common.api.internal.zzbo<?> r3) throws android.os.RemoteException {
        /*
            r2 = this;
            java.util.Map r0 = r3.zzakh()
            com.google.android.gms.common.api.internal.zzck<?> r1 = r2.zzfuc
            java.lang.Object r0 = r0.remove(r1)
            com.google.android.gms.common.api.internal.zzcr r0 = (com.google.android.gms.common.api.internal.zzcr) r0
            if (r0 == 0) goto L1f
            com.google.android.gms.common.api.internal.zzdo<com.google.android.gms.common.api.Api$zzb, ?> r1 = r0.zzftz
            com.google.android.gms.common.api.Api$zze r3 = r3.zzaix()
            com.google.android.gms.tasks.TaskCompletionSource<T> r2 = r2.zzejm
            r1.zzc(r3, r2)
            com.google.android.gms.common.api.internal.zzcq<com.google.android.gms.common.api.Api$zzb, ?> r2 = r0.zzfty
            r2.zzaky()
            return
        L1f:
            com.google.android.gms.tasks.TaskCompletionSource<T> r2 = r2.zzejm
            r3 = 0
            java.lang.Boolean r3 = java.lang.Boolean.valueOf(r3)
            r2.trySetResult(r3)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.common.api.internal.zzf.zzb(com.google.android.gms.common.api.internal.zzbo):void");
    }

    @Override // com.google.android.gms.common.api.internal.zzb, com.google.android.gms.common.api.internal.zza
    public final /* bridge */ /* synthetic */ void zzs(@NonNull Status status) {
        super.zzs(status);
    }
}
