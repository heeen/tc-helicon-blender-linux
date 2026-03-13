package com.airbnb.lottie.model.content;

import com.airbnb.lottie.model.animatable.AnimatableIntegerValue;
import com.airbnb.lottie.model.animatable.AnimatableShapeValue;

/* JADX INFO: loaded from: classes.dex */
public class Mask {
    private final MaskMode maskMode;
    private final AnimatableShapeValue maskPath;
    private final AnimatableIntegerValue opacity;

    public enum MaskMode {
        MaskModeAdd,
        MaskModeSubtract,
        MaskModeIntersect
    }

    public Mask(MaskMode maskMode, AnimatableShapeValue animatableShapeValue, AnimatableIntegerValue animatableIntegerValue) {
        this.maskMode = maskMode;
        this.maskPath = animatableShapeValue;
        this.opacity = animatableIntegerValue;
    }

    public MaskMode getMaskMode() {
        return this.maskMode;
    }

    public AnimatableShapeValue getMaskPath() {
        return this.maskPath;
    }

    public AnimatableIntegerValue getOpacity() {
        return this.opacity;
    }
}
