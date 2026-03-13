package com.airbnb.lottie.model.layer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.ValueCallbackKeyframeAnimation;
import com.airbnb.lottie.value.LottieValueCallback;

/* JADX INFO: loaded from: classes.dex */
public class SolidLayer extends BaseLayer {

    @Nullable
    private BaseKeyframeAnimation<ColorFilter, ColorFilter> colorFilterAnimation;
    private final Layer layerModel;
    private final Paint paint;
    private final Path path;
    private final float[] points;
    private final RectF rect;

    SolidLayer(LottieDrawable lottieDrawable, Layer layer) {
        super(lottieDrawable, layer);
        this.rect = new RectF();
        this.paint = new Paint();
        this.points = new float[8];
        this.path = new Path();
        this.layerModel = layer;
        this.paint.setAlpha(0);
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(layer.getSolidColor());
    }

    @Override // com.airbnb.lottie.model.layer.BaseLayer
    public void drawLayer(Canvas canvas, Matrix matrix, int i) {
        int iAlpha = Color.alpha(this.layerModel.getSolidColor());
        if (iAlpha == 0) {
            return;
        }
        int iIntValue = (int) ((i / 255.0f) * (((iAlpha / 255.0f) * this.transform.getOpacity().getValue().intValue()) / 100.0f) * 255.0f);
        this.paint.setAlpha(iIntValue);
        if (this.colorFilterAnimation != null) {
            this.paint.setColorFilter(this.colorFilterAnimation.getValue());
        }
        if (iIntValue > 0) {
            this.points[0] = 0.0f;
            this.points[1] = 0.0f;
            this.points[2] = this.layerModel.getSolidWidth();
            this.points[3] = 0.0f;
            this.points[4] = this.layerModel.getSolidWidth();
            this.points[5] = this.layerModel.getSolidHeight();
            this.points[6] = 0.0f;
            this.points[7] = this.layerModel.getSolidHeight();
            matrix.mapPoints(this.points);
            this.path.reset();
            this.path.moveTo(this.points[0], this.points[1]);
            this.path.lineTo(this.points[2], this.points[3]);
            this.path.lineTo(this.points[4], this.points[5]);
            this.path.lineTo(this.points[6], this.points[7]);
            this.path.lineTo(this.points[0], this.points[1]);
            this.path.close();
            canvas.drawPath(this.path, this.paint);
        }
    }

    @Override // com.airbnb.lottie.model.layer.BaseLayer, com.airbnb.lottie.animation.content.DrawingContent
    public void getBounds(RectF rectF, Matrix matrix) {
        super.getBounds(rectF, matrix);
        this.rect.set(0.0f, 0.0f, this.layerModel.getSolidWidth(), this.layerModel.getSolidHeight());
        this.boundsMatrix.mapRect(this.rect);
        rectF.set(this.rect);
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
