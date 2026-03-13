package io.github.douglasjunior.androidSimpleTooltip;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.ColorInt;

/* JADX INFO: loaded from: classes.dex */
public class ArrowDrawable extends ColorDrawable {
    public static final int AUTO = 4;
    public static final int BOTTOM = 3;
    public static final int LEFT = 0;
    public static final int RIGHT = 2;
    public static final int TOP = 1;
    private final int mDirection;
    private Path mPath;
    private final Paint mPaint = new Paint(1);
    private final int mBackgroundColor = 0;

    ArrowDrawable(@ColorInt int i, int i2) {
        this.mPaint.setColor(i);
        this.mDirection = i2;
    }

    @Override // android.graphics.drawable.Drawable
    protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        updatePath(rect);
    }

    private synchronized void updatePath(Rect rect) {
        this.mPath = new Path();
        switch (this.mDirection) {
            case 0:
                this.mPath.moveTo(rect.width(), rect.height());
                this.mPath.lineTo(0.0f, rect.height() / 2);
                this.mPath.lineTo(rect.width(), 0.0f);
                this.mPath.lineTo(rect.width(), rect.height());
                break;
            case 1:
                this.mPath.moveTo(0.0f, rect.height());
                this.mPath.lineTo(rect.width() / 2, 0.0f);
                this.mPath.lineTo(rect.width(), rect.height());
                this.mPath.lineTo(0.0f, rect.height());
                break;
            case 2:
                this.mPath.moveTo(0.0f, 0.0f);
                this.mPath.lineTo(rect.width(), rect.height() / 2);
                this.mPath.lineTo(0.0f, rect.height());
                this.mPath.lineTo(0.0f, 0.0f);
                break;
            case 3:
                this.mPath.moveTo(0.0f, 0.0f);
                this.mPath.lineTo(rect.width() / 2, rect.height());
                this.mPath.lineTo(rect.width(), 0.0f);
                this.mPath.lineTo(0.0f, 0.0f);
                break;
        }
        this.mPath.close();
    }

    @Override // android.graphics.drawable.ColorDrawable, android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        canvas.drawColor(this.mBackgroundColor);
        if (this.mPath == null) {
            updatePath(getBounds());
        }
        canvas.drawPath(this.mPath, this.mPaint);
    }

    @Override // android.graphics.drawable.ColorDrawable, android.graphics.drawable.Drawable
    public void setAlpha(int i) {
        this.mPaint.setAlpha(i);
    }

    @Override // android.graphics.drawable.ColorDrawable
    public void setColor(@ColorInt int i) {
        this.mPaint.setColor(i);
    }

    @Override // android.graphics.drawable.ColorDrawable, android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
    }

    @Override // android.graphics.drawable.ColorDrawable, android.graphics.drawable.Drawable
    public int getOpacity() {
        if (this.mPaint.getColorFilter() != null) {
            return -3;
        }
        int color = this.mPaint.getColor() >>> 24;
        if (color != 0) {
            return color != 255 ? -3 : -1;
        }
        return -2;
    }
}
