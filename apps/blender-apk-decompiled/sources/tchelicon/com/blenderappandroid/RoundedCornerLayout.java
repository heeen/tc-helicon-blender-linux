package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.FrameLayout;

/* JADX INFO: loaded from: classes.dex */
public class RoundedCornerLayout extends FrameLayout {
    private static final float CORNER_RADIUS = 40.0f;
    private float cornerRadius;
    private Bitmap maskBitmap;
    private Paint maskPaint;
    private Paint paint;

    public RoundedCornerLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public RoundedCornerLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet, 0);
    }

    public RoundedCornerLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context, attributeSet, i);
    }

    private void init(Context context, AttributeSet attributeSet, int i) {
        this.cornerRadius = TypedValue.applyDimension(1, CORNER_RADIUS, context.getResources().getDisplayMetrics());
        this.paint = new Paint(1);
        this.maskPaint = new Paint(3);
        this.maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        setWillNotDraw(false);
    }

    @Override // android.view.View
    public void draw(Canvas canvas) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(bitmapCreateBitmap);
        super.draw(canvas2);
        if (this.maskBitmap == null) {
            this.maskBitmap = createMask(canvas.getWidth(), canvas.getHeight());
        }
        canvas2.drawBitmap(this.maskBitmap, 0.0f, 0.0f, this.maskPaint);
        canvas.drawBitmap(bitmapCreateBitmap, 0.0f, 0.0f, this.paint);
    }

    private Bitmap createMask(int i, int i2) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint(1);
        paint.setColor(-1);
        float f = i;
        float f2 = i2;
        canvas.drawRect(0.0f, 0.0f, f, f2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRoundRect(new RectF(0.0f, 0.0f, f, f2), this.cornerRadius, this.cornerRadius, paint);
        return bitmapCreateBitmap;
    }
}
