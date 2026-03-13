package com.airbnb.lottie.animation.content;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.TransformKeyframeAnimation;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.model.content.Repeater;
import com.airbnb.lottie.model.layer.BaseLayer;
import com.airbnb.lottie.utils.MiscUtils;
import com.airbnb.lottie.value.LottieValueCallback;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/* JADX INFO: loaded from: classes.dex */
public class RepeaterContent implements DrawingContent, PathContent, GreedyContent, BaseKeyframeAnimation.AnimationListener, KeyPathElementContent {
    private ContentGroup contentGroup;
    private final BaseKeyframeAnimation<Float, Float> copies;
    private final BaseLayer layer;
    private final LottieDrawable lottieDrawable;
    private final String name;
    private final BaseKeyframeAnimation<Float, Float> offset;
    private final TransformKeyframeAnimation transform;
    private final Matrix matrix = new Matrix();
    private final Path path = new Path();

    public RepeaterContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, Repeater repeater) {
        this.lottieDrawable = lottieDrawable;
        this.layer = baseLayer;
        this.name = repeater.getName();
        this.copies = repeater.getCopies().createAnimation();
        baseLayer.addAnimation(this.copies);
        this.copies.addUpdateListener(this);
        this.offset = repeater.getOffset().createAnimation();
        baseLayer.addAnimation(this.offset);
        this.offset.addUpdateListener(this);
        this.transform = repeater.getTransform().createAnimation();
        this.transform.addAnimationsToLayer(baseLayer);
        this.transform.addListener(this);
    }

    @Override // com.airbnb.lottie.animation.content.GreedyContent
    public void absorbContent(ListIterator<Content> listIterator) {
        if (this.contentGroup != null) {
            return;
        }
        while (listIterator.hasPrevious() && listIterator.previous() != this) {
        }
        ArrayList arrayList = new ArrayList();
        while (listIterator.hasPrevious()) {
            arrayList.add(listIterator.previous());
            listIterator.remove();
        }
        Collections.reverse(arrayList);
        this.contentGroup = new ContentGroup(this.lottieDrawable, this.layer, "Repeater", arrayList, null);
    }

    @Override // com.airbnb.lottie.animation.content.Content
    public String getName() {
        return this.name;
    }

    @Override // com.airbnb.lottie.animation.content.Content
    public void setContents(List<Content> list, List<Content> list2) {
        this.contentGroup.setContents(list, list2);
    }

    @Override // com.airbnb.lottie.animation.content.PathContent
    public Path getPath() {
        Path path = this.contentGroup.getPath();
        this.path.reset();
        float fFloatValue = this.copies.getValue().floatValue();
        float fFloatValue2 = this.offset.getValue().floatValue();
        for (int i = ((int) fFloatValue) - 1; i >= 0; i--) {
            this.matrix.set(this.transform.getMatrixForRepeater(i + fFloatValue2));
            this.path.addPath(path, this.matrix);
        }
        return this.path;
    }

    @Override // com.airbnb.lottie.animation.content.DrawingContent
    public void draw(Canvas canvas, Matrix matrix, int i) {
        float fFloatValue = this.copies.getValue().floatValue();
        float fFloatValue2 = this.offset.getValue().floatValue();
        float fFloatValue3 = this.transform.getStartOpacity().getValue().floatValue() / 100.0f;
        float fFloatValue4 = this.transform.getEndOpacity().getValue().floatValue() / 100.0f;
        for (int i2 = ((int) fFloatValue) - 1; i2 >= 0; i2--) {
            this.matrix.set(matrix);
            float f = i2;
            this.matrix.preConcat(this.transform.getMatrixForRepeater(f + fFloatValue2));
            this.contentGroup.draw(canvas, this.matrix, (int) (i * MiscUtils.lerp(fFloatValue3, fFloatValue4, f / fFloatValue)));
        }
    }

    @Override // com.airbnb.lottie.animation.content.DrawingContent
    public void getBounds(RectF rectF, Matrix matrix) {
        this.contentGroup.getBounds(rectF, matrix);
    }

    @Override // com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation.AnimationListener
    public void onValueChanged() {
        this.lottieDrawable.invalidateSelf();
    }

    @Override // com.airbnb.lottie.model.KeyPathElement
    public void resolveKeyPath(KeyPath keyPath, int i, List<KeyPath> list, KeyPath keyPath2) {
        MiscUtils.resolveKeyPath(keyPath, i, list, keyPath2, this);
    }

    @Override // com.airbnb.lottie.model.KeyPathElement
    public <T> void addValueCallback(T t, @Nullable LottieValueCallback<T> lottieValueCallback) {
        if (this.transform.applyValueCallback(t, lottieValueCallback)) {
            return;
        }
        if (t == LottieProperty.REPEATER_COPIES) {
            this.copies.setValueCallback(lottieValueCallback);
        } else if (t == LottieProperty.REPEATER_OFFSET) {
            this.offset.setValueCallback(lottieValueCallback);
        }
    }
}
