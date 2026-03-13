package android.arch.lifecycle;

import android.support.annotation.Nullable;

/* JADX INFO: loaded from: classes.dex */
public interface Observer<T> {
    void onChanged(@Nullable T t);
}
