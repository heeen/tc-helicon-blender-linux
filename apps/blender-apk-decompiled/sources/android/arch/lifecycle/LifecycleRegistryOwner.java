package android.arch.lifecycle;

import android.support.annotation.NonNull;

/* JADX INFO: loaded from: classes.dex */
@Deprecated
public interface LifecycleRegistryOwner extends LifecycleOwner {
    @Override // android.arch.lifecycle.LifecycleOwner
    @NonNull
    LifecycleRegistry getLifecycle();
}
