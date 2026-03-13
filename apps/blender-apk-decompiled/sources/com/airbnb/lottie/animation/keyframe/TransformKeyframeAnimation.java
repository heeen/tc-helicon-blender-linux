package com.airbnb.lottie.animation.keyframe;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.model.animatable.AnimatableTransform;
import com.airbnb.lottie.model.layer.BaseLayer;
import com.airbnb.lottie.value.LottieValueCallback;
import com.airbnb.lottie.value.ScaleXY;

/* JADX INFO: loaded from: classes.dex */
public class TransformKeyframeAnimation {
    private final BaseKeyframeAnimation<PointF, PointF> anchorPoint;

    @Nullable
    private final BaseKeyframeAnimation<?, Float> endOpacity;
    private final Matrix matrix = new Matrix();
    private final BaseKeyframeAnimation<Integer, Integer> opacity;
    private final BaseKeyframeAnimation<?, PointF> position;
    private final BaseKeyframeAnimation<Float, Float> rotation;
    private final BaseKeyframeAnimation<ScaleXY, ScaleXY> scale;

    @Nullable
    private final BaseKeyframeAnimation<?, Float> startOpacity;

    public TransformKeyframeAnimation(AnimatableTransform animatableTransform) {
        this.anchorPoint = animatableTransform.getAnchorPoint().createAnimation();
        this.position = animatableTransform.getPosition().createAnimation();
        this.scale = animatableTransform.getScale().createAnimation();
        this.rotation = animatableTransform.getRotation().createAnimation();
        this.opacity = animatableTransform.getOpacity().createAnimation();
        if (animatableTransform.getStartOpacity() != null) {
            this.startOpacity = animatableTransform.getStartOpacity().createAnimation();
        } else {
            this.startOpacity = null;
        }
        if (animatableTransform.getEndOpacity() != null) {
            this.endOpacity = animatableTransform.getEndOpacity().createAnimation();
        } else {
            this.endOpacity = null;
        }
    }

    public void addAnimationsToLayer(BaseLayer baseLayer) {
        baseLayer.addAnimation(this.anchorPoint);
        baseLayer.addAnimation(this.position);
        baseLayer.addAnimation(this.scale);
        baseLayer.addAnimation(this.rotation);
        baseLayer.addAnimation(this.opacity);
        if (this.startOpacity != null) {
            baseLayer.addAnimation(this.startOpacity);
        }
        if (this.endOpacity != null) {
            baseLayer.addAnimation(this.endOpacity);
        }
    }

    public void addListener(BaseKeyframeAnimation.AnimationListener animationListener) {
        this.anchorPoint.addUpdateListener(animationListener);
        this.position.addUpdateListener(animationListener);
        this.scale.addUpdateListener(animationListener);
        this.rotation.addUpdateListener(animationListener);
        this.opacity.addUpdateListener(animationListener);
        if (this.startOpacity != null) {
            this.startOpacity.addUpdateListener(animationListener);
        }
        if (this.endOpacity != null) {
            this.endOpacity.addUpdateListener(animationListener);
        }
    }

    public void setProgress(float f) {
        this.anchorPoint.setProgress(f);
        this.position.setProgress(f);
        this.scale.setProgress(f);
        this.rotation.setProgress(f);
        this.opacity.setProgress(f);
        if (this.startOpacity != null) {
            this.startOpacity.setProgress(f);
        }
        if (this.endOpacity != null) {
            this.endOpacity.setProgress(f);
        }
    }

    public BaseKeyframeAnimation<?, Integer> getOpacity() {
        return this.opacity;
    }

    @Nullable
    public BaseKeyframeAnimation<?, Float> getStartOpacity() {
        return this.startOpacity;
    }

    @Nullable
    public BaseKeyframeAnimation<?, Float> getEndOpacity() {
        return this.endOpacity;
    }

    public Matrix getMatrix() {
        this.matrix.reset();
        PointF value = this.position.getValue();
        if (value.x != 0.0f || value.y != 0.0f) {
            this.matrix.preTranslate(value.x, value.y);
        }
        float fFloatValue = this.rotation.getValue().floatValue();
        if (fFloatValue != 0.0f) {
            this.matrix.preRotate(fFloatValue);
        }
        ScaleXY value2 = this.scale.getValue();
        if (value2.getScaleX() != 1.0f || value2.getScaleY() != 1.0f) {
            this.matrix.preScale(value2.getScaleX(), value2.getScaleY());
        }
        PointF value3 = this.anchorPoint.getValue();
        if (value3.x != 0.0f || value3.y != 0.0f) {
            this.matrix.preTranslate(-value3.x, -value3.y);
        }
        return this.matrix;
    }

    public Matrix getMatrixForRepeater(float f) {
        PointF value = this.position.getValue();
        PointF value2 = this.anchorPoint.getValue();
        ScaleXY value3 = this.scale.getValue();
        float fFloatValue = this.rotation.getValue().floatValue();
        this.matrix.reset();
        this.matrix.preTranslate(value.x * f, value.y * f);
        double d = f;
        this.matrix.preScale((float) Math.pow(value3.getScaleX(), d), (float) Math.pow(value3.getScaleY(), d));
        this.matrix.preRotate(fFloatValue * f, value2.x, value2.y);
        return this.matrix;
    }

    public <T> boolean applyValueCallback(T t, @Nullable LottieValueCallback<T> lottieValueCallback) {
        if (t == LottieProperty.TRANSFORM_ANCHOR_POINT) {
            this.anchorPoint.setValueCallback(lottieValueCallback);
            return true;
        }
        if (t == LottieProperty.TRANSFORM_POSITION) {
            this.position.setValueCallback(lottieValueCallback);
            return true;
        }
        if (t == LottieProperty.TRANSFORM_SCALE) {
            this.scale.setValueCallback(lottieValueCallback);
            return true;
        }
        if (t == LottieProperty.TRANSFORM_ROTATION) {
            this.rotation.setValueCallback(lottieValueCallback);
            return true;
        }
        if (t == LottieProperty.TRANSFORM_OPACITY) {
            this.opacity.setValueCallback(lottieValueCallback);
            return true;
        }
        if (t == LottieProperty.TRANSFORM_START_OPACITY && this.startOpacity != null) {
            this.startOpacity.setValueCallback(lottieValueCallback);
            return true;
        }
        if (t != LottieProperty.TRANSFORM_END_OPACITY || this.endOpacity == null) {
            return false;
        }
        this.endOpacity.setValueCallback(lottieValueCallback);
        return true;
    }
}
