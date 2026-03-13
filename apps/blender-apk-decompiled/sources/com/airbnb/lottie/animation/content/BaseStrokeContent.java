package com.airbnb.lottie.animation.content;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import com.airbnb.lottie.L;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.ValueCallbackKeyframeAnimation;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.model.animatable.AnimatableFloatValue;
import com.airbnb.lottie.model.animatable.AnimatableIntegerValue;
import com.airbnb.lottie.model.layer.BaseLayer;
import com.airbnb.lottie.utils.MiscUtils;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.LottieValueCallback;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public abstract class BaseStrokeContent implements BaseKeyframeAnimation.AnimationListener, KeyPathElementContent, DrawingContent {

    @Nullable
    private BaseKeyframeAnimation<ColorFilter, ColorFilter> colorFilterAnimation;
    private final List<BaseKeyframeAnimation<?, Float>> dashPatternAnimations;

    @Nullable
    private final BaseKeyframeAnimation<?, Float> dashPatternOffsetAnimation;
    private final float[] dashPatternValues;
    private final LottieDrawable lottieDrawable;
    private final BaseKeyframeAnimation<?, Integer> opacityAnimation;
    private final BaseKeyframeAnimation<?, Float> widthAnimation;
    private final PathMeasure pm = new PathMeasure();
    private final Path path = new Path();
    private final Path trimPathPath = new Path();
    private final RectF rect = new RectF();
    private final List<PathGroup> pathGroups = new ArrayList();
    final Paint paint = new Paint(1);

    BaseStrokeContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, Paint.Cap cap, Paint.Join join, AnimatableIntegerValue animatableIntegerValue, AnimatableFloatValue animatableFloatValue, List<AnimatableFloatValue> list, AnimatableFloatValue animatableFloatValue2) {
        this.lottieDrawable = lottieDrawable;
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeCap(cap);
        this.paint.setStrokeJoin(join);
        this.opacityAnimation = animatableIntegerValue.createAnimation();
        this.widthAnimation = animatableFloatValue.createAnimation();
        if (animatableFloatValue2 == null) {
            this.dashPatternOffsetAnimation = null;
        } else {
            this.dashPatternOffsetAnimation = animatableFloatValue2.createAnimation();
        }
        this.dashPatternAnimations = new ArrayList(list.size());
        this.dashPatternValues = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            this.dashPatternAnimations.add(list.get(i).createAnimation());
        }
        baseLayer.addAnimation(this.opacityAnimation);
        baseLayer.addAnimation(this.widthAnimation);
        for (int i2 = 0; i2 < this.dashPatternAnimations.size(); i2++) {
            baseLayer.addAnimation(this.dashPatternAnimations.get(i2));
        }
        if (this.dashPatternOffsetAnimation != null) {
            baseLayer.addAnimation(this.dashPatternOffsetAnimation);
        }
        this.opacityAnimation.addUpdateListener(this);
        this.widthAnimation.addUpdateListener(this);
        for (int i3 = 0; i3 < list.size(); i3++) {
            this.dashPatternAnimations.get(i3).addUpdateListener(this);
        }
        if (this.dashPatternOffsetAnimation != null) {
            this.dashPatternOffsetAnimation.addUpdateListener(this);
        }
    }

    @Override // com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation.AnimationListener
    public void onValueChanged() {
        this.lottieDrawable.invalidateSelf();
    }

    /* JADX WARN: Removed duplicated region for block: B:21:0x0055  */
    @Override // com.airbnb.lottie.animation.content.Content
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void setContents(java.util.List<com.airbnb.lottie.animation.content.Content> r8, java.util.List<com.airbnb.lottie.animation.content.Content> r9) {
        /*
            r7 = this;
            int r0 = r8.size()
            int r0 = r0 + (-1)
            r1 = 0
            r2 = r1
        L8:
            if (r0 < 0) goto L22
            java.lang.Object r3 = r8.get(r0)
            com.airbnb.lottie.animation.content.Content r3 = (com.airbnb.lottie.animation.content.Content) r3
            boolean r4 = r3 instanceof com.airbnb.lottie.animation.content.TrimPathContent
            if (r4 == 0) goto L1f
            com.airbnb.lottie.animation.content.TrimPathContent r3 = (com.airbnb.lottie.animation.content.TrimPathContent) r3
            com.airbnb.lottie.model.content.ShapeTrimPath$Type r4 = r3.getType()
            com.airbnb.lottie.model.content.ShapeTrimPath$Type r5 = com.airbnb.lottie.model.content.ShapeTrimPath.Type.Individually
            if (r4 != r5) goto L1f
            r2 = r3
        L1f:
            int r0 = r0 + (-1)
            goto L8
        L22:
            if (r2 == 0) goto L27
            r2.addListener(r7)
        L27:
            int r8 = r9.size()
            int r8 = r8 + (-1)
            r0 = r1
        L2e:
            if (r8 < 0) goto L6c
            java.lang.Object r3 = r9.get(r8)
            com.airbnb.lottie.animation.content.Content r3 = (com.airbnb.lottie.animation.content.Content) r3
            boolean r4 = r3 instanceof com.airbnb.lottie.animation.content.TrimPathContent
            if (r4 == 0) goto L55
            r4 = r3
            com.airbnb.lottie.animation.content.TrimPathContent r4 = (com.airbnb.lottie.animation.content.TrimPathContent) r4
            com.airbnb.lottie.model.content.ShapeTrimPath$Type r5 = r4.getType()
            com.airbnb.lottie.model.content.ShapeTrimPath$Type r6 = com.airbnb.lottie.model.content.ShapeTrimPath.Type.Individually
            if (r5 != r6) goto L55
            if (r0 == 0) goto L4c
            java.util.List<com.airbnb.lottie.animation.content.BaseStrokeContent$PathGroup> r3 = r7.pathGroups
            r3.add(r0)
        L4c:
            com.airbnb.lottie.animation.content.BaseStrokeContent$PathGroup r0 = new com.airbnb.lottie.animation.content.BaseStrokeContent$PathGroup
            r0.<init>(r4)
            r4.addListener(r7)
            goto L69
        L55:
            boolean r4 = r3 instanceof com.airbnb.lottie.animation.content.PathContent
            if (r4 == 0) goto L69
            if (r0 != 0) goto L60
            com.airbnb.lottie.animation.content.BaseStrokeContent$PathGroup r0 = new com.airbnb.lottie.animation.content.BaseStrokeContent$PathGroup
            r0.<init>(r2)
        L60:
            java.util.List r4 = com.airbnb.lottie.animation.content.BaseStrokeContent.PathGroup.access$100(r0)
            com.airbnb.lottie.animation.content.PathContent r3 = (com.airbnb.lottie.animation.content.PathContent) r3
            r4.add(r3)
        L69:
            int r8 = r8 + (-1)
            goto L2e
        L6c:
            if (r0 == 0) goto L73
            java.util.List<com.airbnb.lottie.animation.content.BaseStrokeContent$PathGroup> r7 = r7.pathGroups
            r7.add(r0)
        L73:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.airbnb.lottie.animation.content.BaseStrokeContent.setContents(java.util.List, java.util.List):void");
    }

    @Override // com.airbnb.lottie.animation.content.DrawingContent
    public void draw(Canvas canvas, Matrix matrix, int i) {
        L.beginSection("StrokeContent#draw");
        this.paint.setAlpha(MiscUtils.clamp((int) ((((i / 255.0f) * this.opacityAnimation.getValue().intValue()) / 100.0f) * 255.0f), 0, 255));
        this.paint.setStrokeWidth(this.widthAnimation.getValue().floatValue() * Utils.getScale(matrix));
        if (this.paint.getStrokeWidth() <= 0.0f) {
            L.endSection("StrokeContent#draw");
            return;
        }
        applyDashPatternIfNeeded(matrix);
        if (this.colorFilterAnimation != null) {
            this.paint.setColorFilter(this.colorFilterAnimation.getValue());
        }
        for (int i2 = 0; i2 < this.pathGroups.size(); i2++) {
            PathGroup pathGroup = this.pathGroups.get(i2);
            if (pathGroup.trimPath != null) {
                applyTrimPath(canvas, pathGroup, matrix);
            } else {
                L.beginSection("StrokeContent#buildPath");
                this.path.reset();
                for (int size = pathGroup.paths.size() - 1; size >= 0; size--) {
                    this.path.addPath(((PathContent) pathGroup.paths.get(size)).getPath(), matrix);
                }
                L.endSection("StrokeContent#buildPath");
                L.beginSection("StrokeContent#drawPath");
                canvas.drawPath(this.path, this.paint);
                L.endSection("StrokeContent#drawPath");
            }
        }
        L.endSection("StrokeContent#draw");
    }

    /* JADX WARN: Removed duplicated region for block: B:26:0x00f8  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void applyTrimPath(android.graphics.Canvas r12, com.airbnb.lottie.animation.content.BaseStrokeContent.PathGroup r13, android.graphics.Matrix r14) {
        /*
            Method dump skipped, instruction units count: 316
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.airbnb.lottie.animation.content.BaseStrokeContent.applyTrimPath(android.graphics.Canvas, com.airbnb.lottie.animation.content.BaseStrokeContent$PathGroup, android.graphics.Matrix):void");
    }

    @Override // com.airbnb.lottie.animation.content.DrawingContent
    public void getBounds(RectF rectF, Matrix matrix) {
        L.beginSection("StrokeContent#getBounds");
        this.path.reset();
        for (int i = 0; i < this.pathGroups.size(); i++) {
            PathGroup pathGroup = this.pathGroups.get(i);
            for (int i2 = 0; i2 < pathGroup.paths.size(); i2++) {
                this.path.addPath(((PathContent) pathGroup.paths.get(i2)).getPath(), matrix);
            }
        }
        this.path.computeBounds(this.rect, false);
        float fFloatValue = this.widthAnimation.getValue().floatValue() / 2.0f;
        this.rect.set(this.rect.left - fFloatValue, this.rect.top - fFloatValue, this.rect.right + fFloatValue, this.rect.bottom + fFloatValue);
        rectF.set(this.rect);
        rectF.set(rectF.left - 1.0f, rectF.top - 1.0f, rectF.right + 1.0f, rectF.bottom + 1.0f);
        L.endSection("StrokeContent#getBounds");
    }

    private void applyDashPatternIfNeeded(Matrix matrix) {
        L.beginSection("StrokeContent#applyDashPattern");
        if (this.dashPatternAnimations.isEmpty()) {
            L.endSection("StrokeContent#applyDashPattern");
            return;
        }
        float scale = Utils.getScale(matrix);
        for (int i = 0; i < this.dashPatternAnimations.size(); i++) {
            this.dashPatternValues[i] = this.dashPatternAnimations.get(i).getValue().floatValue();
            if (i % 2 == 0) {
                if (this.dashPatternValues[i] < 1.0f) {
                    this.dashPatternValues[i] = 1.0f;
                }
            } else if (this.dashPatternValues[i] < 0.1f) {
                this.dashPatternValues[i] = 0.1f;
            }
            float[] fArr = this.dashPatternValues;
            fArr[i] = fArr[i] * scale;
        }
        this.paint.setPathEffect(new DashPathEffect(this.dashPatternValues, this.dashPatternOffsetAnimation == null ? 0.0f : this.dashPatternOffsetAnimation.getValue().floatValue()));
        L.endSection("StrokeContent#applyDashPattern");
    }

    @Override // com.airbnb.lottie.model.KeyPathElement
    public void resolveKeyPath(KeyPath keyPath, int i, List<KeyPath> list, KeyPath keyPath2) {
        MiscUtils.resolveKeyPath(keyPath, i, list, keyPath2, this);
    }

    @Override // com.airbnb.lottie.model.KeyPathElement
    @CallSuper
    public <T> void addValueCallback(T t, @Nullable LottieValueCallback<T> lottieValueCallback) {
        if (t == LottieProperty.OPACITY) {
            this.opacityAnimation.setValueCallback(lottieValueCallback);
            return;
        }
        if (t == LottieProperty.STROKE_WIDTH) {
            this.widthAnimation.setValueCallback(lottieValueCallback);
        } else if (t == LottieProperty.COLOR_FILTER) {
            if (lottieValueCallback == null) {
                this.colorFilterAnimation = null;
            } else {
                this.colorFilterAnimation = new ValueCallbackKeyframeAnimation(lottieValueCallback);
            }
        }
    }

    private static final class PathGroup {
        private final List<PathContent> paths;

        @Nullable
        private final TrimPathContent trimPath;

        private PathGroup(@Nullable TrimPathContent trimPathContent) {
            this.paths = new ArrayList();
            this.trimPath = trimPathContent;
        }
    }
}
