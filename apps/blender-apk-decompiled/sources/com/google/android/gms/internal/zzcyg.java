package com.google.android.gms.internal;

import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
public final class zzcyg {

    @Hide
    private static Api.zzf<zzcyt> zzegu = new Api.zzf<>();

    @Hide
    private static Api.zzf<zzcyt> zzkln = new Api.zzf<>();

    @Hide
    public static final Api.zza<zzcyt, zzcyk> zzegv = new zzcyh();

    @Hide
    private static Api.zza<zzcyt, Object> zzklo = new zzcyi();
    private static Scope zzemx = new Scope(Scopes.PROFILE);
    private static Scope zzemy = new Scope("email");

    @Hide
    public static final Api<zzcyk> API = new Api<>("SignIn.API", zzegv, zzegu);

    @Hide
    private static Api<Object> zzgpn = new Api<>("SignIn.INTERNAL_API", zzklo, zzkln);
}
