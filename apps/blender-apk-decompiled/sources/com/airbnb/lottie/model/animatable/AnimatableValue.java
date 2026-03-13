package com.airbnb.lottie.model.animatable;

import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;

/* JADX INFO: loaded from: classes.dex */
public interface AnimatableValue<K, A> {
    BaseKeyframeAnimation<K, A> createAnimation();
}
