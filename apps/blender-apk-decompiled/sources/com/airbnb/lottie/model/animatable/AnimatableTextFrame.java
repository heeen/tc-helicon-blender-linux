package com.airbnb.lottie.model.animatable;

import com.airbnb.lottie.animation.keyframe.TextKeyframeAnimation;
import com.airbnb.lottie.model.DocumentData;
import com.airbnb.lottie.value.Keyframe;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class AnimatableTextFrame extends BaseAnimatableValue<DocumentData, DocumentData> {
    @Override // com.airbnb.lottie.model.animatable.BaseAnimatableValue
    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    public AnimatableTextFrame(List<Keyframe<DocumentData>> list) {
        super((List) list);
    }

    @Override // com.airbnb.lottie.model.animatable.AnimatableValue
    public TextKeyframeAnimation createAnimation() {
        return new TextKeyframeAnimation(this.keyframes);
    }
}
