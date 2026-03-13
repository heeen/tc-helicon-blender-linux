package com.google.firebase;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.zzda;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
@Hide
public final class zzb implements zzda {
    @Override // com.google.android.gms.common.api.internal.zzda
    public final Exception zzt(Status status) {
        return status.getStatusCode() == 8 ? new FirebaseException(status.zzaif()) : new FirebaseApiNotAvailableException(status.zzaif());
    }
}
