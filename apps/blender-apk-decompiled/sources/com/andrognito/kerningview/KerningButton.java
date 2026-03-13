package com.andrognito.kerningview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

/* JADX INFO: loaded from: classes.dex */
public class KerningButton extends KerningTextView {
    public KerningButton(Context context) {
        super(context);
    }

    public KerningButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public KerningButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // android.widget.TextView, android.view.View
    public CharSequence getAccessibilityClassName() {
        return Button.class.getName();
    }
}
