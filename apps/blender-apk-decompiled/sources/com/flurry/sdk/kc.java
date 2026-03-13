package com.flurry.sdk;

import android.support.v7.widget.ActivityChooserView;
import java.util.Comparator;

/* JADX INFO: loaded from: classes.dex */
public class kc implements Comparator<Runnable> {
    private static final String a = "kc";

    @Override // java.util.Comparator
    public /* synthetic */ int compare(Runnable runnable, Runnable runnable2) {
        int iA = a(runnable);
        int iA2 = a(runnable2);
        if (iA < iA2) {
            return -1;
        }
        return iA > iA2 ? 1 : 0;
    }

    private static int a(Runnable runnable) {
        if (runnable == null) {
            return ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        }
        if (runnable instanceof kd) {
            lx lxVar = (lx) ((kd) runnable).a();
            return lxVar != null ? lxVar.w : ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        }
        if (!(runnable instanceof lx)) {
            kf.a(6, a, "Unknown runnable class: " + runnable.getClass().getName());
            return ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        }
        return ((lx) runnable).w;
    }
}
