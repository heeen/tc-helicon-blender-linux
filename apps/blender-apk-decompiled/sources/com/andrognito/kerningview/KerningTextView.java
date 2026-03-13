package com.andrognito.kerningview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableString;
import android.text.style.ScaleXSpan;
import android.util.AttributeSet;
import android.widget.TextView;

/* JADX INFO: loaded from: classes.dex */
public class KerningTextView extends AppCompatTextView {
    private final String TAG;
    private float kerningFactor;
    private CharSequence originalText;

    public KerningTextView(Context context) {
        super(context);
        this.TAG = getClass().getSimpleName();
        this.kerningFactor = 0.0f;
        init(null, 0);
    }

    public KerningTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.TAG = getClass().getSimpleName();
        this.kerningFactor = 0.0f;
        init(attributeSet, 0);
    }

    public KerningTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.TAG = getClass().getSimpleName();
        this.kerningFactor = 0.0f;
        init(attributeSet, i);
    }

    private void init(AttributeSet attributeSet, int i) {
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, new int[]{android.R.attr.text});
        TypedArray typedArrayObtainStyledAttributes2 = getContext().obtainStyledAttributes(attributeSet, R.styleable.KerningViews, 0, i);
        try {
            this.kerningFactor = typedArrayObtainStyledAttributes2.getFloat(R.styleable.KerningViews_kv_spacing, 0.0f);
            this.originalText = typedArrayObtainStyledAttributes.getText(0);
            typedArrayObtainStyledAttributes.recycle();
            typedArrayObtainStyledAttributes2.recycle();
            applyKerning();
        } catch (Throwable th) {
            typedArrayObtainStyledAttributes.recycle();
            typedArrayObtainStyledAttributes2.recycle();
            throw th;
        }
    }

    public float getKerningFactor() {
        return this.kerningFactor;
    }

    public void setKerningFactor(float f) {
        this.kerningFactor = f;
        applyKerning();
    }

    @Override // android.widget.TextView
    public void setText(CharSequence charSequence, TextView.BufferType bufferType) {
        this.originalText = charSequence;
        applyKerning();
    }

    @Override // android.widget.TextView
    public CharSequence getText() {
        return this.originalText;
    }

    private void applyKerning() {
        if (this.originalText == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < this.originalText.length()) {
            sb.append(this.originalText.charAt(i));
            i++;
            if (i < this.originalText.length()) {
                sb.append(" ");
            }
        }
        SpannableString spannableString = new SpannableString(sb.toString());
        if (sb.toString().length() > 1) {
            for (int i2 = 1; i2 < sb.toString().length(); i2 += 2) {
                spannableString.setSpan(new ScaleXSpan(this.kerningFactor / 10.0f), i2, i2 + 1, 33);
            }
        }
        super.setText(spannableString, TextView.BufferType.SPANNABLE);
    }

    public class Kerning {
        public static final float LARGE = 6.0f;
        public static final float MEDIUM = 4.0f;
        public static final float NO_KERNING = 0.0f;
        public static final float SMALL = 1.0f;

        public Kerning() {
        }
    }
}
