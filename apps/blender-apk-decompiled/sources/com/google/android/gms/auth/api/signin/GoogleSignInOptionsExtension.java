package com.google.android.gms.auth.api.signin;

import android.os.Bundle;
import android.support.annotation.Nullable;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.Hide;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public interface GoogleSignInOptionsExtension {
    @Hide
    int getExtensionType();

    @Hide
    @Nullable
    List<Scope> getImpliedScopes();

    @Hide
    Bundle toBundle();
}
