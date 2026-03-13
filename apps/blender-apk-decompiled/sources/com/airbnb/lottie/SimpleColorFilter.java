package com.airbnb.lottie;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.ColorInt;

/* JADX INFO: loaded from: classes.dex */
public class SimpleColorFilter extends PorterDuffColorFilter {
    public SimpleColorFilter(@ColorInt int i) {
        super(i, PorterDuff.Mode.SRC_ATOP);
    }
}
