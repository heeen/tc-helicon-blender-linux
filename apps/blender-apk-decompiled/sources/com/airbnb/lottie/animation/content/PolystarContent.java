package com.airbnb.lottie.animation.content;

import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.model.content.PolystarShape;
import com.airbnb.lottie.model.content.ShapeTrimPath;
import com.airbnb.lottie.model.layer.BaseLayer;
import com.airbnb.lottie.utils.MiscUtils;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.LottieValueCallback;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class PolystarContent implements PathContent, BaseKeyframeAnimation.AnimationListener, KeyPathElementContent {
    private static final float POLYGON_MAGIC_NUMBER = 0.25f;
    private static final float POLYSTAR_MAGIC_NUMBER = 0.47829f;

    @Nullable
    private final BaseKeyframeAnimation<?, Float> innerRadiusAnimation;

    @Nullable
    private final BaseKeyframeAnimation<?, Float> innerRoundednessAnimation;
    private boolean isPathValid;
    private final LottieDrawable lottieDrawable;
    private final String name;
    private final BaseKeyframeAnimation<?, Float> outerRadiusAnimation;
    private final BaseKeyframeAnimation<?, Float> outerRoundednessAnimation;
    private final Path path = new Path();
    private final BaseKeyframeAnimation<?, Float> pointsAnimation;
    private final BaseKeyframeAnimation<?, PointF> positionAnimation;
    private final BaseKeyframeAnimation<?, Float> rotationAnimation;

    @Nullable
    private TrimPathContent trimPath;
    private final PolystarShape.Type type;

    public PolystarContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, PolystarShape polystarShape) {
        this.lottieDrawable = lottieDrawable;
        this.name = polystarShape.getName();
        this.type = polystarShape.getType();
        this.pointsAnimation = polystarShape.getPoints().createAnimation();
        this.positionAnimation = polystarShape.getPosition().createAnimation();
        this.rotationAnimation = polystarShape.getRotation().createAnimation();
        this.outerRadiusAnimation = polystarShape.getOuterRadius().createAnimation();
        this.outerRoundednessAnimation = polystarShape.getOuterRoundedness().createAnimation();
        if (this.type == PolystarShape.Type.Star) {
            this.innerRadiusAnimation = polystarShape.getInnerRadius().createAnimation();
            this.innerRoundednessAnimation = polystarShape.getInnerRoundedness().createAnimation();
        } else {
            this.innerRadiusAnimation = null;
            this.innerRoundednessAnimation = null;
        }
        baseLayer.addAnimation(this.pointsAnimation);
        baseLayer.addAnimation(this.positionAnimation);
        baseLayer.addAnimation(this.rotationAnimation);
        baseLayer.addAnimation(this.outerRadiusAnimation);
        baseLayer.addAnimation(this.outerRoundednessAnimation);
        if (this.type == PolystarShape.Type.Star) {
            baseLayer.addAnimation(this.innerRadiusAnimation);
            baseLayer.addAnimation(this.innerRoundednessAnimation);
        }
        this.pointsAnimation.addUpdateListener(this);
        this.positionAnimation.addUpdateListener(this);
        this.rotationAnimation.addUpdateListener(this);
        this.outerRadiusAnimation.addUpdateListener(this);
        this.outerRoundednessAnimation.addUpdateListener(this);
        if (this.type == PolystarShape.Type.Star) {
            this.outerRadiusAnimation.addUpdateListener(this);
            this.outerRoundednessAnimation.addUpdateListener(this);
        }
    }

    @Override // com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation.AnimationListener
    public void onValueChanged() {
        invalidate();
    }

    private void invalidate() {
        this.isPathValid = false;
        this.lottieDrawable.invalidateSelf();
    }

    @Override // com.airbnb.lottie.animation.content.Content
    public void setContents(List<Content> list, List<Content> list2) {
        for (int i = 0; i < list.size(); i++) {
            Content content = list.get(i);
            if (content instanceof TrimPathContent) {
                TrimPathContent trimPathContent = (TrimPathContent) content;
                if (trimPathContent.getType() == ShapeTrimPath.Type.Simultaneously) {
                    this.trimPath = trimPathContent;
                    this.trimPath.addListener(this);
                }
            }
        }
    }

    @Override // com.airbnb.lottie.animation.content.PathContent
    public Path getPath() {
        if (this.isPathValid) {
            return this.path;
        }
        this.path.reset();
        switch (this.type) {
            case Star:
                createStarPath();
                break;
            case Polygon:
                createPolygonPath();
                break;
        }
        this.path.close();
        Utils.applyTrimPathIfNeeded(this.path, this.trimPath);
        this.isPathValid = true;
        return this.path;
    }

    @Override // com.airbnb.lottie.animation.content.Content
    public String getName() {
        return this.name;
    }

    private void createStarPath() {
        double d;
        int i;
        float fSin;
        double d2;
        float fCos;
        float f;
        float f2;
        float f3;
        float f4;
        float f5;
        float f6;
        float f7;
        float f8;
        float f9;
        float f10;
        double d3;
        float fFloatValue = this.pointsAnimation.getValue().floatValue();
        double radians = Math.toRadians((this.rotationAnimation == null ? 0.0d : this.rotationAnimation.getValue().floatValue()) - 90.0d);
        double d4 = fFloatValue;
        float f11 = (float) (6.283185307179586d / d4);
        float f12 = f11 / 2.0f;
        float f13 = fFloatValue - ((int) fFloatValue);
        int i2 = (f13 > 0.0f ? 1 : (f13 == 0.0f ? 0 : -1));
        if (i2 != 0) {
            radians += (double) ((1.0f - f13) * f12);
        }
        float fFloatValue2 = this.outerRadiusAnimation.getValue().floatValue();
        float fFloatValue3 = this.innerRadiusAnimation.getValue().floatValue();
        float fFloatValue4 = this.innerRoundednessAnimation != null ? this.innerRoundednessAnimation.getValue().floatValue() / 100.0f : 0.0f;
        float fFloatValue5 = this.outerRoundednessAnimation != null ? this.outerRoundednessAnimation.getValue().floatValue() / 100.0f : 0.0f;
        if (i2 != 0) {
            f = ((fFloatValue2 - fFloatValue3) * f13) + fFloatValue3;
            i = i2;
            double d5 = f;
            d = d4;
            fCos = (float) (d5 * Math.cos(radians));
            fSin = (float) (d5 * Math.sin(radians));
            this.path.moveTo(fCos, fSin);
            d2 = radians + ((double) ((f11 * f13) / 2.0f));
        } else {
            d = d4;
            i = i2;
            double d6 = fFloatValue2;
            float fCos2 = (float) (Math.cos(radians) * d6);
            fSin = (float) (d6 * Math.sin(radians));
            this.path.moveTo(fCos2, fSin);
            d2 = radians + ((double) f12);
            fCos = fCos2;
            f = 0.0f;
        }
        double dCeil = Math.ceil(d) * 2.0d;
        int i3 = 0;
        double d7 = d2;
        boolean z = false;
        while (true) {
            double d8 = i3;
            if (d8 < dCeil) {
                float f14 = z ? fFloatValue2 : fFloatValue3;
                if (f == 0.0f || d8 != dCeil - 2.0d) {
                    f2 = f14;
                    f3 = f12;
                } else {
                    f2 = f14;
                    f3 = (f11 * f13) / 2.0f;
                }
                if (f == 0.0f || d8 != dCeil - 1.0d) {
                    f4 = f3;
                    f5 = f11;
                    f6 = f2;
                } else {
                    f4 = f3;
                    f5 = f11;
                    f6 = f;
                }
                double d9 = f6;
                float fCos3 = (float) (d9 * Math.cos(d7));
                float fSin2 = (float) (d9 * Math.sin(d7));
                if (fFloatValue4 == 0.0f && fFloatValue5 == 0.0f) {
                    this.path.lineTo(fCos3, fSin2);
                    f10 = f12;
                    d3 = dCeil;
                    f7 = fFloatValue4;
                    f8 = fFloatValue5;
                    f9 = f;
                } else {
                    f7 = fFloatValue4;
                    f8 = fFloatValue5;
                    f9 = f;
                    double dAtan2 = (float) (Math.atan2(fSin, fCos) - 1.5707963267948966d);
                    float f15 = fCos;
                    float fCos4 = (float) Math.cos(dAtan2);
                    float fSin3 = (float) Math.sin(dAtan2);
                    f10 = f12;
                    d3 = dCeil;
                    double dAtan22 = (float) (Math.atan2(fSin2, fCos3) - 1.5707963267948966d);
                    float fCos5 = (float) Math.cos(dAtan22);
                    float fSin4 = (float) Math.sin(dAtan22);
                    float f16 = z ? f7 : f8;
                    float f17 = z ? f8 : f7;
                    float f18 = z ? fFloatValue3 : fFloatValue2;
                    float f19 = z ? fFloatValue2 : fFloatValue3;
                    float f20 = f18 * f16 * POLYSTAR_MAGIC_NUMBER;
                    float f21 = fCos4 * f20;
                    float f22 = f20 * fSin3;
                    float f23 = f19 * f17 * POLYSTAR_MAGIC_NUMBER;
                    float f24 = fCos5 * f23;
                    float f25 = f23 * fSin4;
                    if (i != 0) {
                        if (i3 == 0) {
                            f21 *= f13;
                            f22 *= f13;
                        } else if (d8 == d3 - 1.0d) {
                            f24 *= f13;
                            f25 *= f13;
                        }
                    }
                    this.path.cubicTo(f15 - f21, fSin - f22, fCos3 + f24, fSin2 + f25, fCos3, fSin2);
                }
                d7 += (double) f4;
                z = !z;
                i3++;
                fSin = fSin2;
                fCos = fCos3;
                f11 = f5;
                fFloatValue4 = f7;
                fFloatValue5 = f8;
                f = f9;
                f12 = f10;
                dCeil = d3;
            } else {
                PointF value = this.positionAnimation.getValue();
                this.path.offset(value.x, value.y);
                this.path.close();
                return;
            }
        }
    }

    private void createPolygonPath() {
        int i;
        double d;
        double d2;
        double d3;
        int iFloor = (int) Math.floor(this.pointsAnimation.getValue().floatValue());
        double radians = Math.toRadians((this.rotationAnimation == null ? 0.0d : this.rotationAnimation.getValue().floatValue()) - 90.0d);
        double d4 = iFloor;
        float fFloatValue = this.outerRoundednessAnimation.getValue().floatValue() / 100.0f;
        float fFloatValue2 = this.outerRadiusAnimation.getValue().floatValue();
        double d5 = fFloatValue2;
        float fCos = (float) (Math.cos(radians) * d5);
        float fSin = (float) (Math.sin(radians) * d5);
        this.path.moveTo(fCos, fSin);
        double d6 = (float) (6.283185307179586d / d4);
        double d7 = radians + d6;
        double dCeil = Math.ceil(d4);
        int i2 = 0;
        while (i2 < dCeil) {
            float fCos2 = (float) (Math.cos(d7) * d5);
            double d8 = dCeil;
            float fSin2 = (float) (d5 * Math.sin(d7));
            if (fFloatValue != 0.0f) {
                d2 = d5;
                i = i2;
                d = d7;
                double dAtan2 = (float) (Math.atan2(fSin, fCos) - 1.5707963267948966d);
                float fCos3 = (float) Math.cos(dAtan2);
                float fSin3 = (float) Math.sin(dAtan2);
                d3 = d6;
                double dAtan22 = (float) (Math.atan2(fSin2, fCos2) - 1.5707963267948966d);
                float fCos4 = (float) Math.cos(dAtan22);
                float fSin4 = (float) Math.sin(dAtan22);
                float f = fFloatValue2 * fFloatValue * POLYGON_MAGIC_NUMBER;
                this.path.cubicTo(fCos - (fCos3 * f), fSin - (fSin3 * f), fCos2 + (fCos4 * f), fSin2 + (f * fSin4), fCos2, fSin2);
            } else {
                i = i2;
                d = d7;
                d2 = d5;
                d3 = d6;
                this.path.lineTo(fCos2, fSin2);
            }
            d7 = d + d3;
            i2 = i + 1;
            fSin = fSin2;
            fCos = fCos2;
            dCeil = d8;
            d5 = d2;
            d6 = d3;
        }
        PointF value = this.positionAnimation.getValue();
        this.path.offset(value.x, value.y);
        this.path.close();
    }

    @Override // com.airbnb.lottie.model.KeyPathElement
    public void resolveKeyPath(KeyPath keyPath, int i, List<KeyPath> list, KeyPath keyPath2) {
        MiscUtils.resolveKeyPath(keyPath, i, list, keyPath2, this);
    }

    @Override // com.airbnb.lottie.model.KeyPathElement
    public <T> void addValueCallback(T t, @Nullable LottieValueCallback<T> lottieValueCallback) {
        if (t == LottieProperty.POLYSTAR_POINTS) {
            this.pointsAnimation.setValueCallback(lottieValueCallback);
            return;
        }
        if (t == LottieProperty.POLYSTAR_ROTATION) {
            this.rotationAnimation.setValueCallback(lottieValueCallback);
            return;
        }
        if (t == LottieProperty.POSITION) {
            this.positionAnimation.setValueCallback(lottieValueCallback);
            return;
        }
        if (t == LottieProperty.POLYSTAR_INNER_RADIUS && this.innerRadiusAnimation != null) {
            this.innerRadiusAnimation.setValueCallback(lottieValueCallback);
            return;
        }
        if (t == LottieProperty.POLYSTAR_OUTER_RADIUS) {
            this.outerRadiusAnimation.setValueCallback(lottieValueCallback);
            return;
        }
        if (t == LottieProperty.POLYSTAR_INNER_ROUNDEDNESS && this.innerRoundednessAnimation != null) {
            this.innerRoundednessAnimation.setValueCallback(lottieValueCallback);
        } else if (t == LottieProperty.POLYSTAR_OUTER_ROUNDEDNESS) {
            this.outerRoundednessAnimation.setValueCallback(lottieValueCallback);
        }
    }
}
