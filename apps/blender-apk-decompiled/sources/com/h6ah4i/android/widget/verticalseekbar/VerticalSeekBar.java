package com.h6ah4i.android.widget.verticalseekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ProgressBar;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public class VerticalSeekBar extends AppCompatSeekBar {
    public static final int ROTATION_ANGLE_CW_270 = 270;
    public static final int ROTATION_ANGLE_CW_90 = 90;
    private boolean mIsDragging;
    private Method mMethodSetProgressFromUser;
    private int mRotationAngle;
    private Drawable mThumb_;

    private static boolean isValidRotationAngle(int i) {
        return i == 90 || i == 270;
    }

    public VerticalSeekBar(Context context) {
        super(context);
        this.mRotationAngle = 90;
        initialize(context, null, 0, 0);
    }

    public VerticalSeekBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRotationAngle = 90;
        initialize(context, attributeSet, 0, 0);
    }

    public VerticalSeekBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mRotationAngle = 90;
        initialize(context, attributeSet, i, 0);
    }

    private void initialize(Context context, AttributeSet attributeSet, int i, int i2) {
        ViewCompat.setLayoutDirection(this, 0);
        if (attributeSet != null) {
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.VerticalSeekBar, i, i2);
            int integer = typedArrayObtainStyledAttributes.getInteger(R.styleable.VerticalSeekBar_seekBarRotation, 0);
            if (isValidRotationAngle(integer)) {
                this.mRotationAngle = integer;
            }
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    @Override // android.widget.AbsSeekBar
    public void setThumb(Drawable drawable) {
        this.mThumb_ = drawable;
        super.setThumb(drawable);
    }

    @Override // android.widget.AbsSeekBar, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (useViewRotation()) {
            return onTouchEventUseViewRotation(motionEvent);
        }
        return onTouchEventTraditionalRotation(motionEvent);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private boolean onTouchEventTraditionalRotation(MotionEvent motionEvent) {
        if (!isEnabled()) {
            return false;
        }
        switch (motionEvent.getAction()) {
            case 0:
                setPressed(true);
                onStartTrackingTouch();
                trackTouchEvent(motionEvent);
                attemptClaimDrag(true);
                invalidate();
                return true;
            case 1:
                if (this.mIsDragging) {
                    trackTouchEvent(motionEvent);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(motionEvent);
                    onStopTrackingTouch();
                    attemptClaimDrag(false);
                }
                invalidate();
                return true;
            case 2:
                if (this.mIsDragging) {
                    trackTouchEvent(motionEvent);
                }
                return true;
            case 3:
                if (this.mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate();
                return true;
            default:
                return true;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:9:0x0016  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean onTouchEventUseViewRotation(android.view.MotionEvent r3) {
        /*
            r2 = this;
            boolean r0 = super.onTouchEvent(r3)
            if (r0 == 0) goto L1a
            int r3 = r3.getAction()
            r1 = 3
            if (r3 == r1) goto L16
            switch(r3) {
                case 0: goto L11;
                case 1: goto L16;
                default: goto L10;
            }
        L10:
            goto L1a
        L11:
            r3 = 1
            r2.attemptClaimDrag(r3)
            goto L1a
        L16:
            r3 = 0
            r2.attemptClaimDrag(r3)
        L1a:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar.onTouchEventUseViewRotation(android.view.MotionEvent):boolean");
    }

    private void trackTouchEvent(MotionEvent motionEvent) {
        int paddingLeft = super.getPaddingLeft();
        int paddingRight = super.getPaddingRight();
        int height = (getHeight() - paddingLeft) - paddingRight;
        int y = (int) motionEvent.getY();
        int i = this.mRotationAngle;
        float f = 0.0f;
        float f2 = i != 90 ? i != 270 ? 0.0f : r2 - y : y - paddingLeft;
        if (f2 >= 0.0f && height != 0) {
            float f3 = height;
            f = f2 > f3 ? 1.0f : f2 / f3;
        }
        _setProgressFromUser((int) (f * getMax()), true);
    }

    private void attemptClaimDrag(boolean z) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(z);
        }
    }

    private void onStartTrackingTouch() {
        this.mIsDragging = true;
    }

    private void onStopTrackingTouch() {
        this.mIsDragging = false;
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x001c  */
    /* JADX WARN: Removed duplicated region for block: B:14:0x001e  */
    /* JADX WARN: Removed duplicated region for block: B:17:0x0023  */
    @Override // android.widget.AbsSeekBar, android.view.View, android.view.KeyEvent.Callback
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean onKeyDown(int r5, android.view.KeyEvent r6) {
        /*
            r4 = this;
            boolean r0 = r4.isEnabled()
            if (r0 == 0) goto L39
            r0 = -1
            r1 = 0
            r2 = 1
            switch(r5) {
                case 19: goto L16;
                case 20: goto Lf;
                case 21: goto Le;
                case 22: goto Le;
                default: goto Lc;
            }
        Lc:
            r0 = r1
            goto L21
        Le:
            return r1
        Lf:
            int r1 = r4.mRotationAngle
            r3 = 90
            if (r1 != r3) goto L1e
            goto L1c
        L16:
            int r1 = r4.mRotationAngle
            r3 = 270(0x10e, float:3.78E-43)
            if (r1 != r3) goto L1e
        L1c:
            r1 = r2
            goto L1f
        L1e:
            r1 = r0
        L1f:
            r0 = r1
            r1 = r2
        L21:
            if (r1 == 0) goto L39
            int r5 = r4.getKeyProgressIncrement()
            int r6 = r4.getProgress()
            int r0 = r0 * r5
            int r6 = r6 + r0
            if (r6 < 0) goto L38
            int r5 = r4.getMax()
            if (r6 > r5) goto L38
            r4._setProgressFromUser(r6, r2)
        L38:
            return r2
        L39:
            boolean r4 = super.onKeyDown(r5, r6)
            return r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar.onKeyDown(int, android.view.KeyEvent):boolean");
    }

    @Override // android.widget.ProgressBar
    public synchronized void setProgress(int i) {
        super.setProgress(i);
        if (!useViewRotation()) {
            refreshThumb();
        }
    }

    private synchronized void _setProgressFromUser(int i, boolean z) {
        if (this.mMethodSetProgressFromUser == null) {
            try {
                Method declaredMethod = ProgressBar.class.getDeclaredMethod("setProgress", Integer.TYPE, Boolean.TYPE);
                declaredMethod.setAccessible(true);
                this.mMethodSetProgressFromUser = declaredMethod;
            } catch (NoSuchMethodException unused) {
            }
        }
        if (this.mMethodSetProgressFromUser != null) {
            try {
                this.mMethodSetProgressFromUser.invoke(this, Integer.valueOf(i), Boolean.valueOf(z));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException unused2) {
            }
        } else {
            super.setProgress(i);
        }
        refreshThumb();
    }

    @Override // android.widget.AbsSeekBar, android.widget.ProgressBar, android.view.View
    protected synchronized void onMeasure(int i, int i2) {
        if (useViewRotation()) {
            super.onMeasure(i, i2);
        } else {
            super.onMeasure(i2, i);
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            if (isInEditMode() && layoutParams != null && layoutParams.height >= 0) {
                setMeasuredDimension(super.getMeasuredHeight(), layoutParams.height);
            } else {
                setMeasuredDimension(super.getMeasuredHeight(), super.getMeasuredWidth());
            }
        }
    }

    @Override // android.widget.AbsSeekBar, android.widget.ProgressBar, android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        if (useViewRotation()) {
            super.onSizeChanged(i, i2, i3, i4);
        } else {
            super.onSizeChanged(i2, i, i4, i3);
        }
    }

    @Override // android.support.v7.widget.AppCompatSeekBar, android.widget.AbsSeekBar, android.widget.ProgressBar, android.view.View
    protected synchronized void onDraw(Canvas canvas) {
        if (!useViewRotation()) {
            int i = this.mRotationAngle;
            if (i == 90) {
                canvas.rotate(90.0f);
                canvas.translate(0.0f, -super.getWidth());
            } else if (i == 270) {
                canvas.rotate(-90.0f);
                canvas.translate(-super.getHeight(), 0.0f);
            }
        }
        super.onDraw(canvas);
    }

    public int getRotationAngle() {
        return this.mRotationAngle;
    }

    public void setRotationAngle(int i) {
        if (!isValidRotationAngle(i)) {
            throw new IllegalArgumentException("Invalid angle specified :" + i);
        }
        if (this.mRotationAngle == i) {
            return;
        }
        this.mRotationAngle = i;
        if (useViewRotation()) {
            VerticalSeekBarWrapper wrapper = getWrapper();
            if (wrapper != null) {
                wrapper.applyViewRotation();
                return;
            }
            return;
        }
        requestLayout();
    }

    private void refreshThumb() {
        onSizeChanged(super.getWidth(), super.getHeight(), 0, 0);
    }

    boolean useViewRotation() {
        return (Build.VERSION.SDK_INT >= 11) && !isInEditMode();
    }

    private VerticalSeekBarWrapper getWrapper() {
        ViewParent parent = getParent();
        if (parent instanceof VerticalSeekBarWrapper) {
            return (VerticalSeekBarWrapper) parent;
        }
        return null;
    }
}
