package com.google.firebase.internal.api;

import android.support.annotation.NonNull;
import com.google.android.gms.common.annotation.KeepForSdk;
import com.google.android.gms.common.internal.Hide;
import com.google.firebase.FirebaseException;

/* JADX INFO: loaded from: classes.dex */
@Hide
@KeepForSdk
public class FirebaseNoSignedInUserException extends FirebaseException {
    public FirebaseNoSignedInUserException(@NonNull String str) {
        super(str);
    }
}
