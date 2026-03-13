package io.github.douglasjunior.androidSimpleTooltip;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.v4.view.ViewCompat;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
@SuppressLint({"ViewConstructor"})
public class OverlayView extends View {
    public static final int HIGHLIGHT_SHAPE_OVAL = 0;
    public static final int HIGHLIGHT_SHAPE_RECTANGULAR = 1;
    private static final int mDefaultOverlayAlphaRes = R.integer.simpletooltip_overlay_alpha;
    private Bitmap bitmap;
    private final int highlightShape;
    private boolean invalidated;
    private View mAnchorView;
    private final float mOffset;

    @Override // android.view.View
    public boolean isInEditMode() {
        return true;
    }

    OverlayView(Context context, View view, int i, float f) {
        super(context);
        this.invalidated = true;
        this.mAnchorView = view;
        this.mOffset = f;
        this.highlightShape = i;
    }

    @Override // android.view.View
    protected void dispatchDraw(Canvas canvas) {
        if (this.invalidated || this.bitmap == null || this.bitmap.isRecycled()) {
            createWindowFrame();
        }
        if (this.bitmap == null || this.bitmap.isRecycled()) {
            return;
        }
        canvas.drawBitmap(this.bitmap, 0.0f, 0.0f, (Paint) null);
    }

    private void createWindowFrame() {
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        if (measuredWidth <= 0 || measuredHeight <= 0) {
            return;
        }
        if (this.bitmap != null && !this.bitmap.isRecycled()) {
            this.bitmap.recycle();
        }
        this.bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(this.bitmap);
        RectF rectF = new RectF(0.0f, 0.0f, measuredWidth, measuredHeight);
        Paint paint = new Paint(1);
        paint.setColor(ViewCompat.MEASURED_STATE_MASK);
        paint.setAntiAlias(true);
        paint.setAlpha(getResources().getInteger(mDefaultOverlayAlphaRes));
        canvas.drawRect(rectF, paint);
        paint.setColor(0);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        RectF rectFCalculeRectInWindow = SimpleTooltipUtils.calculeRectInWindow(this.mAnchorView);
        RectF rectFCalculeRectInWindow2 = SimpleTooltipUtils.calculeRectInWindow(this);
        float f = rectFCalculeRectInWindow.left - rectFCalculeRectInWindow2.left;
        float f2 = rectFCalculeRectInWindow.top - rectFCalculeRectInWindow2.top;
        RectF rectF2 = new RectF(f - this.mOffset, f2 - this.mOffset, f + this.mAnchorView.getMeasuredWidth() + this.mOffset, f2 + this.mAnchorView.getMeasuredHeight() + this.mOffset);
        if (this.highlightShape == 1) {
            canvas.drawRect(rectF2, paint);
        } else {
            canvas.drawOval(rectF2, paint);
        }
        this.invalidated = false;
    }

    @Override // android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.invalidated = true;
    }

    public View getAnchorView() {
        return this.mAnchorView;
    }

    public void setAnchorView(View view) {
        this.mAnchorView = view;
        invalidate();
    }
}
