package com.airbnb.lottie.model.layer;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.CallSuper;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.util.Log;
import com.airbnb.lottie.L;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.animation.content.Content;
import com.airbnb.lottie.animation.content.DrawingContent;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.FloatKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.MaskKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.TransformKeyframeAnimation;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.model.KeyPathElement;
import com.airbnb.lottie.model.content.Mask;
import com.airbnb.lottie.model.content.ShapeData;
import com.airbnb.lottie.model.layer.Layer;
import com.airbnb.lottie.value.LottieValueCallback;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public abstract class BaseLayer implements DrawingContent, BaseKeyframeAnimation.AnimationListener, KeyPathElement {
    private static final int SAVE_FLAGS = 19;
    private final String drawTraceName;
    final Layer layerModel;
    final LottieDrawable lottieDrawable;

    @Nullable
    private MaskKeyframeAnimation mask;

    @Nullable
    private BaseLayer matteLayer;

    @Nullable
    private BaseLayer parentLayer;
    private List<BaseLayer> parentLayers;
    final TransformKeyframeAnimation transform;
    private final Path path = new Path();
    private final Matrix matrix = new Matrix();
    private final Paint contentPaint = new Paint(1);
    private final Paint addMaskPaint = new Paint(1);
    private final Paint subtractMaskPaint = new Paint(1);
    private final Paint mattePaint = new Paint(1);
    private final Paint clearPaint = new Paint();
    private final RectF rect = new RectF();
    private final RectF maskBoundsRect = new RectF();
    private final RectF matteBoundsRect = new RectF();
    private final RectF tempMaskBoundsRect = new RectF();
    final Matrix boundsMatrix = new Matrix();
    private final List<BaseKeyframeAnimation<?, ?>> animations = new ArrayList();
    private boolean visible = true;

    abstract void drawLayer(Canvas canvas, Matrix matrix, int i);

    void resolveChildKeyPath(KeyPath keyPath, int i, List<KeyPath> list, KeyPath keyPath2) {
    }

    @Override // com.airbnb.lottie.animation.content.Content
    public void setContents(List<Content> list, List<Content> list2) {
    }

    @Nullable
    static BaseLayer forModel(Layer layer, LottieDrawable lottieDrawable, LottieComposition lottieComposition) {
        switch (layer.getLayerType()) {
            case Shape:
                return new ShapeLayer(lottieDrawable, layer);
            case PreComp:
                return new CompositionLayer(lottieDrawable, layer, lottieComposition.getPrecomps(layer.getRefId()), lottieComposition);
            case Solid:
                return new SolidLayer(lottieDrawable, layer);
            case Image:
                return new ImageLayer(lottieDrawable, layer);
            case Null:
                return new NullLayer(lottieDrawable, layer);
            case Text:
                return new TextLayer(lottieDrawable, layer);
            default:
                L.warn("Unknown layer type " + layer.getLayerType());
                return null;
        }
    }

    BaseLayer(LottieDrawable lottieDrawable, Layer layer) {
        this.lottieDrawable = lottieDrawable;
        this.layerModel = layer;
        this.drawTraceName = layer.getName() + "#draw";
        this.clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.addMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        this.subtractMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        if (layer.getMatteType() == Layer.MatteType.Invert) {
            this.mattePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        } else {
            this.mattePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        }
        this.transform = layer.getTransform().createAnimation();
        this.transform.addListener(this);
        if (layer.getMasks() != null && !layer.getMasks().isEmpty()) {
            this.mask = new MaskKeyframeAnimation(layer.getMasks());
            for (BaseKeyframeAnimation<ShapeData, Path> baseKeyframeAnimation : this.mask.getMaskAnimations()) {
                addAnimation(baseKeyframeAnimation);
                baseKeyframeAnimation.addUpdateListener(this);
            }
            for (BaseKeyframeAnimation<Integer, Integer> baseKeyframeAnimation2 : this.mask.getOpacityAnimations()) {
                addAnimation(baseKeyframeAnimation2);
                baseKeyframeAnimation2.addUpdateListener(this);
            }
        }
        setupInOutAnimations();
    }

    @Override // com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation.AnimationListener
    public void onValueChanged() {
        invalidateSelf();
    }

    Layer getLayerModel() {
        return this.layerModel;
    }

    void setMatteLayer(@Nullable BaseLayer baseLayer) {
        this.matteLayer = baseLayer;
    }

    boolean hasMatteOnThisLayer() {
        return this.matteLayer != null;
    }

    void setParentLayer(@Nullable BaseLayer baseLayer) {
        this.parentLayer = baseLayer;
    }

    private void setupInOutAnimations() {
        if (!this.layerModel.getInOutKeyframes().isEmpty()) {
            final FloatKeyframeAnimation floatKeyframeAnimation = new FloatKeyframeAnimation(this.layerModel.getInOutKeyframes());
            floatKeyframeAnimation.setIsDiscrete();
            floatKeyframeAnimation.addUpdateListener(new BaseKeyframeAnimation.AnimationListener() { // from class: com.airbnb.lottie.model.layer.BaseLayer.1
                @Override // com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation.AnimationListener
                public void onValueChanged() {
                    BaseLayer.this.setVisible(floatKeyframeAnimation.getValue().floatValue() == 1.0f);
                }
            });
            setVisible(floatKeyframeAnimation.getValue().floatValue() == 1.0f);
            addAnimation(floatKeyframeAnimation);
            return;
        }
        setVisible(true);
    }

    private void invalidateSelf() {
        this.lottieDrawable.invalidateSelf();
    }

    public void addAnimation(BaseKeyframeAnimation<?, ?> baseKeyframeAnimation) {
        this.animations.add(baseKeyframeAnimation);
    }

    @Override // com.airbnb.lottie.animation.content.DrawingContent
    @CallSuper
    public void getBounds(RectF rectF, Matrix matrix) {
        this.boundsMatrix.set(matrix);
        this.boundsMatrix.preConcat(this.transform.getMatrix());
    }

    @Override // com.airbnb.lottie.animation.content.DrawingContent
    @SuppressLint({"WrongConstant"})
    public void draw(Canvas canvas, Matrix matrix, int i) {
        L.beginSection(this.drawTraceName);
        if (!this.visible) {
            L.endSection(this.drawTraceName);
            return;
        }
        buildParentLayerListIfNeeded();
        L.beginSection("Layer#parentMatrix");
        this.matrix.reset();
        this.matrix.set(matrix);
        for (int size = this.parentLayers.size() - 1; size >= 0; size--) {
            this.matrix.preConcat(this.parentLayers.get(size).transform.getMatrix());
        }
        L.endSection("Layer#parentMatrix");
        int iIntValue = (int) ((((i / 255.0f) * this.transform.getOpacity().getValue().intValue()) / 100.0f) * 255.0f);
        if (!hasMatteOnThisLayer() && !hasMasksOnThisLayer()) {
            this.matrix.preConcat(this.transform.getMatrix());
            L.beginSection("Layer#drawLayer");
            drawLayer(canvas, this.matrix, iIntValue);
            L.endSection("Layer#drawLayer");
            recordRenderTime(L.endSection(this.drawTraceName));
            return;
        }
        L.beginSection("Layer#computeBounds");
        this.rect.set(0.0f, 0.0f, 0.0f, 0.0f);
        getBounds(this.rect, this.matrix);
        intersectBoundsWithMatte(this.rect, this.matrix);
        this.matrix.preConcat(this.transform.getMatrix());
        intersectBoundsWithMask(this.rect, this.matrix);
        this.rect.set(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight());
        L.endSection("Layer#computeBounds");
        L.beginSection("Layer#saveLayer");
        canvas.saveLayer(this.rect, this.contentPaint, 31);
        L.endSection("Layer#saveLayer");
        clearCanvas(canvas);
        L.beginSection("Layer#drawLayer");
        drawLayer(canvas, this.matrix, iIntValue);
        L.endSection("Layer#drawLayer");
        if (hasMasksOnThisLayer()) {
            applyMasks(canvas, this.matrix);
        }
        if (hasMatteOnThisLayer()) {
            L.beginSection("Layer#drawMatte");
            L.beginSection("Layer#saveLayer");
            canvas.saveLayer(this.rect, this.mattePaint, 19);
            L.endSection("Layer#saveLayer");
            clearCanvas(canvas);
            this.matteLayer.draw(canvas, matrix, iIntValue);
            L.beginSection("Layer#restoreLayer");
            canvas.restore();
            L.endSection("Layer#restoreLayer");
            L.endSection("Layer#drawMatte");
        }
        L.beginSection("Layer#restoreLayer");
        canvas.restore();
        L.endSection("Layer#restoreLayer");
        recordRenderTime(L.endSection(this.drawTraceName));
    }

    private void recordRenderTime(float f) {
        this.lottieDrawable.getComposition().getPerformanceTracker().recordRenderTime(this.layerModel.getName(), f);
    }

    private void clearCanvas(Canvas canvas) {
        L.beginSection("Layer#clearLayer");
        canvas.drawRect(this.rect.left - 1.0f, this.rect.top - 1.0f, this.rect.right + 1.0f, this.rect.bottom + 1.0f, this.clearPaint);
        L.endSection("Layer#clearLayer");
    }

    /* JADX WARN: Removed duplicated region for block: B:7:0x001b  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void intersectBoundsWithMask(android.graphics.RectF r10, android.graphics.Matrix r11) {
        /*
            Method dump skipped, instruction units count: 210
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.airbnb.lottie.model.layer.BaseLayer.intersectBoundsWithMask(android.graphics.RectF, android.graphics.Matrix):void");
    }

    private void intersectBoundsWithMatte(RectF rectF, Matrix matrix) {
        if (hasMatteOnThisLayer() && this.layerModel.getMatteType() != Layer.MatteType.Invert) {
            this.matteLayer.getBounds(this.matteBoundsRect, matrix);
            rectF.set(Math.max(rectF.left, this.matteBoundsRect.left), Math.max(rectF.top, this.matteBoundsRect.top), Math.min(rectF.right, this.matteBoundsRect.right), Math.min(rectF.bottom, this.matteBoundsRect.bottom));
        }
    }

    private void applyMasks(Canvas canvas, Matrix matrix) {
        applyMasks(canvas, matrix, Mask.MaskMode.MaskModeAdd);
        applyMasks(canvas, matrix, Mask.MaskMode.MaskModeIntersect);
        applyMasks(canvas, matrix, Mask.MaskMode.MaskModeSubtract);
    }

    @SuppressLint({"WrongConstant"})
    private void applyMasks(Canvas canvas, Matrix matrix, Mask.MaskMode maskMode) {
        Paint paint;
        boolean z;
        switch (maskMode) {
            case MaskModeSubtract:
                paint = this.subtractMaskPaint;
                break;
            case MaskModeIntersect:
                Log.w(L.TAG, "Animation contains intersect masks. They are not supported but will be treated like add masks.");
            default:
                paint = this.addMaskPaint;
                break;
        }
        int size = this.mask.getMasks().size();
        int i = 0;
        while (true) {
            if (i >= size) {
                z = false;
            } else if (this.mask.getMasks().get(i).getMaskMode() == maskMode) {
                z = true;
            } else {
                i++;
            }
        }
        if (z) {
            L.beginSection("Layer#drawMask");
            L.beginSection("Layer#saveLayer");
            canvas.saveLayer(this.rect, paint, 19);
            L.endSection("Layer#saveLayer");
            clearCanvas(canvas);
            for (int i2 = 0; i2 < size; i2++) {
                if (this.mask.getMasks().get(i2).getMaskMode() == maskMode) {
                    this.path.set(this.mask.getMaskAnimations().get(i2).getValue());
                    this.path.transform(matrix);
                    BaseKeyframeAnimation<Integer, Integer> baseKeyframeAnimation = this.mask.getOpacityAnimations().get(i2);
                    int alpha = this.contentPaint.getAlpha();
                    this.contentPaint.setAlpha((int) (baseKeyframeAnimation.getValue().intValue() * 2.55f));
                    canvas.drawPath(this.path, this.contentPaint);
                    this.contentPaint.setAlpha(alpha);
                }
            }
            L.beginSection("Layer#restoreLayer");
            canvas.restore();
            L.endSection("Layer#restoreLayer");
            L.endSection("Layer#drawMask");
        }
    }

    boolean hasMasksOnThisLayer() {
        return (this.mask == null || this.mask.getMaskAnimations().isEmpty()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setVisible(boolean z) {
        if (z != this.visible) {
            this.visible = z;
            invalidateSelf();
        }
    }

    void setProgress(@FloatRange(from = 0.0d, to = 1.0d) float f) {
        this.transform.setProgress(f);
        if (this.layerModel.getTimeStretch() != 0.0f) {
            f /= this.layerModel.getTimeStretch();
        }
        if (this.matteLayer != null) {
            this.matteLayer.setProgress(this.matteLayer.layerModel.getTimeStretch() * f);
        }
        for (int i = 0; i < this.animations.size(); i++) {
            this.animations.get(i).setProgress(f);
        }
    }

    private void buildParentLayerListIfNeeded() {
        if (this.parentLayers != null) {
            return;
        }
        if (this.parentLayer == null) {
            this.parentLayers = Collections.emptyList();
            return;
        }
        this.parentLayers = new ArrayList();
        for (BaseLayer baseLayer = this.parentLayer; baseLayer != null; baseLayer = baseLayer.parentLayer) {
            this.parentLayers.add(baseLayer);
        }
    }

    @Override // com.airbnb.lottie.animation.content.Content
    public String getName() {
        return this.layerModel.getName();
    }

    @Override // com.airbnb.lottie.model.KeyPathElement
    public void resolveKeyPath(KeyPath keyPath, int i, List<KeyPath> list, KeyPath keyPath2) {
        if (keyPath.matches(getName(), i)) {
            if (!"__container".equals(getName())) {
                keyPath2 = keyPath2.addKey(getName());
                if (keyPath.fullyResolvesTo(getName(), i)) {
                    list.add(keyPath2.resolve(this));
                }
            }
            if (keyPath.propagateToChildren(getName(), i)) {
                resolveChildKeyPath(keyPath, i + keyPath.incrementDepthBy(getName(), i), list, keyPath2);
            }
        }
    }

    @Override // com.airbnb.lottie.model.KeyPathElement
    @CallSuper
    public <T> void addValueCallback(T t, @Nullable LottieValueCallback<T> lottieValueCallback) {
        this.transform.applyValueCallback(t, lottieValueCallback);
    }
}
