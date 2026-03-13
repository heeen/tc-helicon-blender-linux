package com.airbnb.lottie.model.content;

import android.graphics.Path;
import android.support.annotation.Nullable;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.animation.content.Content;
import com.airbnb.lottie.animation.content.FillContent;
import com.airbnb.lottie.model.animatable.AnimatableColorValue;
import com.airbnb.lottie.model.animatable.AnimatableIntegerValue;
import com.airbnb.lottie.model.layer.BaseLayer;

/* JADX INFO: loaded from: classes.dex */
public class ShapeFill implements ContentModel {

    @Nullable
    private final AnimatableColorValue color;
    private final boolean fillEnabled;
    private final Path.FillType fillType;
    private final String name;

    @Nullable
    private final AnimatableIntegerValue opacity;

    public ShapeFill(String str, boolean z, Path.FillType fillType, @Nullable AnimatableColorValue animatableColorValue, @Nullable AnimatableIntegerValue animatableIntegerValue) {
        this.name = str;
        this.fillEnabled = z;
        this.fillType = fillType;
        this.color = animatableColorValue;
        this.opacity = animatableIntegerValue;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    public AnimatableColorValue getColor() {
        return this.color;
    }

    @Nullable
    public AnimatableIntegerValue getOpacity() {
        return this.opacity;
    }

    public Path.FillType getFillType() {
        return this.fillType;
    }

    @Override // com.airbnb.lottie.model.content.ContentModel
    public Content toContent(LottieDrawable lottieDrawable, BaseLayer baseLayer) {
        return new FillContent(lottieDrawable, baseLayer, this);
    }

    public String toString() {
        return "ShapeFill{color=, fillEnabled=" + this.fillEnabled + '}';
    }
}
