package com.h6ah4i.android.widget.verticalseekbar;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/* JADX INFO: loaded from: classes.dex */
public class VerticalSeekBarWrapper extends FrameLayout {
    public VerticalSeekBarWrapper(Context context) {
        this(context, null, 0);
    }

    public VerticalSeekBarWrapper(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public VerticalSeekBarWrapper(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        if (useViewRotation()) {
            onSizeChangedUseViewRotation(i, i2, i3, i4);
        } else {
            onSizeChangedTraditionalRotation(i, i2, i3, i4);
        }
    }

    private void onSizeChangedTraditionalRotation(int i, int i2, int i3, int i4) {
        VerticalSeekBar childSeekBar = getChildSeekBar();
        if (childSeekBar != null) {
            int paddingLeft = getPaddingLeft() + getPaddingRight();
            int paddingTop = getPaddingTop() + getPaddingBottom();
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) childSeekBar.getLayoutParams();
            layoutParams.width = -2;
            int i5 = i2 - paddingTop;
            layoutParams.height = Math.max(0, i5);
            childSeekBar.setLayoutParams(layoutParams);
            childSeekBar.measure(0, 0);
            int measuredWidth = childSeekBar.getMeasuredWidth();
            int i6 = i - paddingLeft;
            childSeekBar.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0, i6), Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(Math.max(0, i5), 1073741824));
            layoutParams.gravity = 51;
            layoutParams.leftMargin = (Math.max(0, i6) - measuredWidth) / 2;
            childSeekBar.setLayoutParams(layoutParams);
        }
        super.onSizeChanged(i, i2, i3, i4);
    }

    private void onSizeChangedUseViewRotation(int i, int i2, int i3, int i4) {
        VerticalSeekBar childSeekBar = getChildSeekBar();
        if (childSeekBar != null) {
            childSeekBar.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0, i2 - (getPaddingTop() + getPaddingBottom())), 1073741824), View.MeasureSpec.makeMeasureSpec(Math.max(0, i - (getPaddingLeft() + getPaddingRight())), Integer.MIN_VALUE));
        }
        applyViewRotation(i, i2);
        super.onSizeChanged(i, i2, i3, i4);
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int measuredWidth;
        int measuredHeight;
        VerticalSeekBar childSeekBar = getChildSeekBar();
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        if (childSeekBar != null && mode != 1073741824) {
            int paddingLeft = getPaddingLeft() + getPaddingRight();
            int paddingTop = getPaddingTop() + getPaddingBottom();
            int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(Math.max(0, size - paddingLeft), mode);
            int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(Math.max(0, size2 - paddingTop), mode2);
            if (useViewRotation()) {
                childSeekBar.measure(iMakeMeasureSpec2, iMakeMeasureSpec);
                measuredWidth = childSeekBar.getMeasuredHeight();
                measuredHeight = childSeekBar.getMeasuredWidth();
            } else {
                childSeekBar.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
                measuredWidth = childSeekBar.getMeasuredWidth();
                measuredHeight = childSeekBar.getMeasuredHeight();
            }
            setMeasuredDimension(ViewCompat.resolveSizeAndState(measuredWidth + paddingLeft, i, 0), ViewCompat.resolveSizeAndState(measuredHeight + paddingTop, i2, 0));
            return;
        }
        super.onMeasure(i, i2);
    }

    void applyViewRotation() {
        applyViewRotation(getWidth(), getHeight());
    }

    private void applyViewRotation(int i, int i2) {
        VerticalSeekBar childSeekBar = getChildSeekBar();
        if (childSeekBar != null) {
            boolean z = ViewCompat.getLayoutDirection(this) == 0;
            int rotationAngle = childSeekBar.getRotationAngle();
            int measuredWidth = childSeekBar.getMeasuredWidth();
            int measuredHeight = childSeekBar.getMeasuredHeight();
            int paddingLeft = getPaddingLeft() + getPaddingRight();
            int paddingTop = getPaddingTop() + getPaddingBottom();
            float fMax = (Math.max(0, i - paddingLeft) - measuredHeight) * 0.5f;
            ViewGroup.LayoutParams layoutParams = childSeekBar.getLayoutParams();
            layoutParams.width = Math.max(0, i2 - paddingTop);
            layoutParams.height = -2;
            childSeekBar.setLayoutParams(layoutParams);
            ViewCompat.setPivotX(childSeekBar, z ? 0.0f : Math.max(0, r10));
            ViewCompat.setPivotY(childSeekBar, 0.0f);
            if (rotationAngle == 90) {
                ViewCompat.setRotation(childSeekBar, 90.0f);
                if (z) {
                    ViewCompat.setTranslationX(childSeekBar, measuredHeight + fMax);
                    ViewCompat.setTranslationY(childSeekBar, 0.0f);
                    return;
                } else {
                    ViewCompat.setTranslationX(childSeekBar, -fMax);
                    ViewCompat.setTranslationY(childSeekBar, measuredWidth);
                    return;
                }
            }
            if (rotationAngle != 270) {
                return;
            }
            ViewCompat.setRotation(childSeekBar, 270.0f);
            if (z) {
                ViewCompat.setTranslationX(childSeekBar, fMax);
                ViewCompat.setTranslationY(childSeekBar, measuredWidth);
            } else {
                ViewCompat.setTranslationX(childSeekBar, -(measuredHeight + fMax));
                ViewCompat.setTranslationY(childSeekBar, 0.0f);
            }
        }
    }

    private VerticalSeekBar getChildSeekBar() {
        View childAt = getChildCount() > 0 ? getChildAt(0) : null;
        if (childAt instanceof VerticalSeekBar) {
            return (VerticalSeekBar) childAt;
        }
        return null;
    }

    private boolean useViewRotation() {
        VerticalSeekBar childSeekBar = getChildSeekBar();
        if (childSeekBar != null) {
            return childSeekBar.useViewRotation();
        }
        return false;
    }
}
