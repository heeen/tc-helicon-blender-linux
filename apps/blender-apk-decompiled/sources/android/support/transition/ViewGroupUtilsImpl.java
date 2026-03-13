package android.support.transition;

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.ViewGroup;

/* JADX INFO: loaded from: classes.dex */
@RequiresApi(14)
interface ViewGroupUtilsImpl {
    ViewGroupOverlayImpl getOverlay(@NonNull ViewGroup viewGroup);

    void suppressLayout(@NonNull ViewGroup viewGroup, boolean z);
}
