package com.airbnb.lottie.animation.content;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.v4.util.LongSparseArray;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.model.content.GradientColor;
import com.airbnb.lottie.model.content.GradientStroke;
import com.airbnb.lottie.model.content.GradientType;
import com.airbnb.lottie.model.layer.BaseLayer;

/* JADX INFO: loaded from: classes.dex */
public class GradientStrokeContent extends BaseStrokeContent {
    private static final int CACHE_STEPS_MS = 32;
    private final RectF boundsRect;
    private final int cacheSteps;
    private final BaseKeyframeAnimation<GradientColor, GradientColor> colorAnimation;
    private final BaseKeyframeAnimation<PointF, PointF> endPointAnimation;
    private final LongSparseArray<LinearGradient> linearGradientCache;
    private final String name;
    private final LongSparseArray<RadialGradient> radialGradientCache;
    private final BaseKeyframeAnimation<PointF, PointF> startPointAnimation;
    private final GradientType type;

    public GradientStrokeContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, GradientStroke gradientStroke) {
        super(lottieDrawable, baseLayer, gradientStroke.getCapType().toPaintCap(), gradientStroke.getJoinType().toPaintJoin(), gradientStroke.getOpacity(), gradientStroke.getWidth(), gradientStroke.getLineDashPattern(), gradientStroke.getDashOffset());
        this.linearGradientCache = new LongSparseArray<>();
        this.radialGradientCache = new LongSparseArray<>();
        this.boundsRect = new RectF();
        this.name = gradientStroke.getName();
        this.type = gradientStroke.getGradientType();
        this.cacheSteps = (int) (lottieDrawable.getComposition().getDuration() / 32.0f);
        this.colorAnimation = gradientStroke.getGradientColor().createAnimation();
        this.colorAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.colorAnimation);
        this.startPointAnimation = gradientStroke.getStartPoint().createAnimation();
        this.startPointAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.startPointAnimation);
        this.endPointAnimation = gradientStroke.getEndPoint().createAnimation();
        this.endPointAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.endPointAnimation);
    }

    @Override // com.airbnb.lottie.animation.content.BaseStrokeContent, com.airbnb.lottie.animation.content.DrawingContent
    public void draw(Canvas canvas, Matrix matrix, int i) {
        getBounds(this.boundsRect, matrix);
        if (this.type == GradientType.Linear) {
            this.paint.setShader(getLinearGradient());
        } else {
            this.paint.setShader(getRadialGradient());
        }
        super.draw(canvas, matrix, i);
    }

    @Override // com.airbnb.lottie.animation.content.Content
    public String getName() {
        return this.name;
    }

    private LinearGradient getLinearGradient() {
        long gradientHash = getGradientHash();
        LinearGradient linearGradient = this.linearGradientCache.get(gradientHash);
        if (linearGradient != null) {
            return linearGradient;
        }
        PointF value = this.startPointAnimation.getValue();
        PointF value2 = this.endPointAnimation.getValue();
        GradientColor value3 = this.colorAnimation.getValue();
        LinearGradient linearGradient2 = new LinearGradient((int) (this.boundsRect.left + (this.boundsRect.width() / 2.0f) + value.x), (int) (this.boundsRect.top + (this.boundsRect.height() / 2.0f) + value.y), (int) (this.boundsRect.left + (this.boundsRect.width() / 2.0f) + value2.x), (int) (this.boundsRect.top + (this.boundsRect.height() / 2.0f) + value2.y), value3.getColors(), value3.getPositions(), Shader.TileMode.CLAMP);
        this.linearGradientCache.put(gradientHash, linearGradient2);
        return linearGradient2;
    }

    private RadialGradient getRadialGradient() {
        long gradientHash = getGradientHash();
        RadialGradient radialGradient = this.radialGradientCache.get(gradientHash);
        if (radialGradient != null) {
            return radialGradient;
        }
        PointF value = this.startPointAnimation.getValue();
        PointF value2 = this.endPointAnimation.getValue();
        GradientColor value3 = this.colorAnimation.getValue();
        int[] colors = value3.getColors();
        float[] positions = value3.getPositions();
        RadialGradient radialGradient2 = new RadialGradient((int) (this.boundsRect.left + (this.boundsRect.width() / 2.0f) + value.x), (int) (this.boundsRect.top + (this.boundsRect.height() / 2.0f) + value.y), (float) Math.hypot(((int) ((this.boundsRect.left + (this.boundsRect.width() / 2.0f)) + value2.x)) - r4, ((int) ((this.boundsRect.top + (this.boundsRect.height() / 2.0f)) + value2.y)) - r0), colors, positions, Shader.TileMode.CLAMP);
        this.radialGradientCache.put(gradientHash, radialGradient2);
        return radialGradient2;
    }

    private int getGradientHash() {
        int iRound = Math.round(this.startPointAnimation.getProgress() * this.cacheSteps);
        int iRound2 = Math.round(this.endPointAnimation.getProgress() * this.cacheSteps);
        int iRound3 = Math.round(this.colorAnimation.getProgress() * this.cacheSteps);
        int i = iRound != 0 ? 527 * iRound : 17;
        if (iRound2 != 0) {
            i = i * 31 * iRound2;
        }
        return iRound3 != 0 ? i * 31 * iRound3 : i;
    }
}
