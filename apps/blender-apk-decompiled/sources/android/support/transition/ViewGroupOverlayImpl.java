package android.support.transition;

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
@RequiresApi(14)
interface ViewGroupOverlayImpl extends ViewOverlayImpl {
    void add(@NonNull View view);

    void remove(@NonNull View view);
}
