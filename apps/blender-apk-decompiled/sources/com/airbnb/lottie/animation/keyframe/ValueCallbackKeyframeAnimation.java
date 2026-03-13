package com.airbnb.lottie.animation.keyframe;

import com.airbnb.lottie.value.Keyframe;
import com.airbnb.lottie.value.LottieFrameInfo;
import com.airbnb.lottie.value.LottieValueCallback;
import java.util.Collections;

/* JADX INFO: loaded from: classes.dex */
public class ValueCallbackKeyframeAnimation<K, A> extends BaseKeyframeAnimation<K, A> {
    private final LottieFrameInfo<A> frameInfo;

    @Override // com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation
    float getEndProgress() {
        return 1.0f;
    }

    public ValueCallbackKeyframeAnimation(LottieValueCallback<A> lottieValueCallback) {
        super(Collections.emptyList());
        this.frameInfo = new LottieFrameInfo<>();
        setValueCallback(lottieValueCallback);
    }

    @Override // com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation
    public void notifyListeners() {
        if (this.valueCallback != null) {
            super.notifyListeners();
        }
    }

    @Override // com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation
    public A getValue() {
        return this.valueCallback.getValueInternal(0.0f, 0.0f, null, null, getProgress(), getProgress(), getProgress());
    }

    @Override // com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation
    A getValue(Keyframe<K> keyframe, float f) {
        return getValue();
    }
}
