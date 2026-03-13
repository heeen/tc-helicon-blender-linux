package com.google.android.gms.common.api.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Result;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public interface zzcc {
    ConnectionResult blockingConnect();

    ConnectionResult blockingConnect(long j, TimeUnit timeUnit);

    void connect();

    void disconnect();

    void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

    @Nullable
    ConnectionResult getConnectionResult(@NonNull Api<?> api);

    boolean isConnected();

    boolean isConnecting();

    boolean zza(zzcu zzcuVar);

    void zzaia();

    void zzais();

    <A extends Api.zzb, R extends Result, T extends zzm<R, A>> T zzd(@NonNull T t);

    <A extends Api.zzb, T extends zzm<? extends Result, A>> T zze(@NonNull T t);
}
