package android.support.transition;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

/* JADX INFO: loaded from: classes.dex */
@RequiresApi(14)
interface ViewOverlayImpl {
    void add(@NonNull Drawable drawable);

    void clear();

    void remove(@NonNull Drawable drawable);
}
