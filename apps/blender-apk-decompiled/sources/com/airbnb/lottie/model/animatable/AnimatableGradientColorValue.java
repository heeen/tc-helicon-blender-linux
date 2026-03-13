package com.airbnb.lottie.model.animatable;

import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.GradientColorKeyframeAnimation;
import com.airbnb.lottie.model.content.GradientColor;
import com.airbnb.lottie.value.Keyframe;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class AnimatableGradientColorValue extends BaseAnimatableValue<GradientColor, GradientColor> {
    @Override // com.airbnb.lottie.model.animatable.BaseAnimatableValue
    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    public AnimatableGradientColorValue(List<Keyframe<GradientColor>> list) {
        super((List) list);
    }

    @Override // com.airbnb.lottie.model.animatable.AnimatableValue
    public BaseKeyframeAnimation<GradientColor, GradientColor> createAnimation() {
        return new GradientColorKeyframeAnimation(this.keyframes);
    }
}
