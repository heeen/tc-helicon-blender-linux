package com.airbnb.lottie.model.layer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.ValueCallbackKeyframeAnimation;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.LottieValueCallback;

/* JADX INFO: loaded from: classes.dex */
public class ImageLayer extends BaseLayer {

    @Nullable
    private BaseKeyframeAnimation<ColorFilter, ColorFilter> colorFilterAnimation;
    private final Rect dst;
    private final Paint paint;
    private final Rect src;

    ImageLayer(LottieDrawable lottieDrawable, Layer layer) {
        super(lottieDrawable, layer);
        this.paint = new Paint(3);
        this.src = new Rect();
        this.dst = new Rect();
    }

    @Override // com.airbnb.lottie.model.layer.BaseLayer
    public void drawLayer(@NonNull Canvas canvas, Matrix matrix, int i) {
        Bitmap bitmap = getBitmap();
        if (bitmap == null) {
            return;
        }
        float fDpScale = Utils.dpScale();
        this.paint.setAlpha(i);
        if (this.colorFilterAnimation != null) {
            this.paint.setColorFilter(this.colorFilterAnimation.getValue());
        }
        canvas.save();
        canvas.concat(matrix);
        this.src.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        this.dst.set(0, 0, (int) (bitmap.getWidth() * fDpScale), (int) (bitmap.getHeight() * fDpScale));
        canvas.drawBitmap(bitmap, this.src, this.dst, this.paint);
        canvas.restore();
    }

    @Override // com.airbnb.lottie.model.layer.BaseLayer, com.airbnb.lottie.animation.content.DrawingContent
    public void getBounds(RectF rectF, Matrix matrix) {
        super.getBounds(rectF, matrix);
        if (getBitmap() != null) {
            rectF.set(rectF.left, rectF.top, Math.min(rectF.right, r6.getWidth()), Math.min(rectF.bottom, r6.getHeight()));
            this.boundsMatrix.mapRect(rectF);
        }
    }

    @Nullable
    private Bitmap getBitmap() {
        return this.lottieDrawable.getImageAsset(this.layerModel.getRefId());
    }

    @Override // com.airbnb.lottie.model.layer.BaseLayer, com.airbnb.lottie.model.KeyPathElement
    public <T> void addValueCallback(T t, @Nullable LottieValueCallback<T> lottieValueCallback) {
        super.addValueCallback(t, lottieValueCallback);
        if (t == LottieProperty.COLOR_FILTER) {
            if (lottieValueCallback == null) {
                this.colorFilterAnimation = null;
            } else {
                this.colorFilterAnimation = new ValueCallbackKeyframeAnimation(lottieValueCallback);
            }
        }
    }
}
