package com.airbnb.lottie.utils;

import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.Choreographer;
import com.airbnb.lottie.LottieComposition;

/* JADX INFO: loaded from: classes.dex */
public class LottieValueAnimator extends BaseLottieAnimator implements Choreographer.FrameCallback {

    @Nullable
    private LottieComposition composition;
    private float speed = 1.0f;
    private boolean speedReversedForRepeatMode = false;
    private long lastFrameTimeNs = 0;
    private float frame = 0.0f;
    private int repeatCount = 0;
    private float minFrame = -2.1474836E9f;
    private float maxFrame = 2.1474836E9f;

    @VisibleForTesting
    protected boolean isRunning = false;

    @Override // android.animation.ValueAnimator
    public Object getAnimatedValue() {
        return Float.valueOf(getAnimatedValueAbsolute());
    }

    @FloatRange(from = 0.0d, to = 1.0d)
    public float getAnimatedValueAbsolute() {
        if (this.composition == null) {
            return 0.0f;
        }
        return (this.frame - this.composition.getStartFrame()) / (this.composition.getEndFrame() - this.composition.getStartFrame());
    }

    @Override // android.animation.ValueAnimator
    @FloatRange(from = 0.0d, to = 1.0d)
    public float getAnimatedFraction() {
        if (this.composition == null) {
            return 0.0f;
        }
        if (isReversed()) {
            return (getMaxFrame() - this.frame) / (getMaxFrame() - getMinFrame());
        }
        return (this.frame - getMinFrame()) / (getMaxFrame() - getMinFrame());
    }

    @Override // android.animation.ValueAnimator, android.animation.Animator
    public long getDuration() {
        if (this.composition == null) {
            return 0L;
        }
        return (long) this.composition.getDuration();
    }

    public float getFrame() {
        return this.frame;
    }

    @Override // android.animation.ValueAnimator, android.animation.Animator
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override // android.view.Choreographer.FrameCallback
    public void doFrame(long j) {
        postFrameCallback();
        if (this.composition == null || !isRunning()) {
            return;
        }
        long jNanoTime = System.nanoTime();
        float frameDurationNs = (jNanoTime - this.lastFrameTimeNs) / getFrameDurationNs();
        float f = this.frame;
        if (isReversed()) {
            frameDurationNs = -frameDurationNs;
        }
        this.frame = f + frameDurationNs;
        boolean z = !MiscUtils.contains(this.frame, getMinFrame(), getMaxFrame());
        this.frame = MiscUtils.clamp(this.frame, getMinFrame(), getMaxFrame());
        this.lastFrameTimeNs = jNanoTime;
        notifyUpdate();
        if (z) {
            if (getRepeatCount() != -1 && this.repeatCount >= getRepeatCount()) {
                this.frame = getMaxFrame();
                removeFrameCallback();
                notifyEnd(isReversed());
            } else {
                notifyRepeat();
                this.repeatCount++;
                if (getRepeatMode() == 2) {
                    this.speedReversedForRepeatMode = !this.speedReversedForRepeatMode;
                    reverseAnimationSpeed();
                } else {
                    this.frame = isReversed() ? getMaxFrame() : getMinFrame();
                }
                this.lastFrameTimeNs = jNanoTime;
            }
        }
        verifyFrame();
    }

    private float getFrameDurationNs() {
        if (this.composition == null) {
            return Float.MAX_VALUE;
        }
        return (1.0E9f / this.composition.getFrameRate()) / Math.abs(this.speed);
    }

    public void setComposition(LottieComposition lottieComposition) {
        this.composition = lottieComposition;
        setMinAndMaxFrames((int) Math.max(this.minFrame, lottieComposition.getStartFrame()), (int) Math.min(this.maxFrame, lottieComposition.getEndFrame()));
        setFrame((int) this.frame);
        this.lastFrameTimeNs = System.nanoTime();
    }

    public void setFrame(int i) {
        float f = i;
        if (this.frame == f) {
            return;
        }
        this.frame = MiscUtils.clamp(f, getMinFrame(), getMaxFrame());
        this.lastFrameTimeNs = System.nanoTime();
        notifyUpdate();
    }

    public void setMinFrame(int i) {
        setMinAndMaxFrames(i, (int) this.maxFrame);
    }

    public void setMaxFrame(int i) {
        setMinAndMaxFrames((int) this.minFrame, i);
    }

    public void setMinAndMaxFrames(int i, int i2) {
        float startFrame = this.composition == null ? Float.MIN_VALUE : this.composition.getStartFrame();
        float endFrame = this.composition == null ? Float.MAX_VALUE : this.composition.getEndFrame();
        float f = i;
        this.minFrame = MiscUtils.clamp(f, startFrame, endFrame);
        float f2 = i2;
        this.maxFrame = MiscUtils.clamp(f2, startFrame, endFrame);
        setFrame((int) MiscUtils.clamp(this.frame, f, f2));
    }

    public void reverseAnimationSpeed() {
        setSpeed(-getSpeed());
    }

    public void setSpeed(float f) {
        this.speed = f;
    }

    public float getSpeed() {
        return this.speed;
    }

    @Override // android.animation.ValueAnimator
    public void setRepeatMode(int i) {
        super.setRepeatMode(i);
        if (i == 2 || !this.speedReversedForRepeatMode) {
            return;
        }
        this.speedReversedForRepeatMode = false;
        reverseAnimationSpeed();
    }

    public void playAnimation() {
        notifyStart(isReversed());
        setFrame((int) (isReversed() ? getMaxFrame() : getMinFrame()));
        this.lastFrameTimeNs = System.nanoTime();
        this.repeatCount = 0;
        postFrameCallback();
    }

    public void endAnimation() {
        removeFrameCallback();
        notifyEnd(isReversed());
    }

    public void pauseAnimation() {
        removeFrameCallback();
    }

    public void resumeAnimation() {
        postFrameCallback();
        this.lastFrameTimeNs = System.nanoTime();
        if (isReversed() && getFrame() == getMinFrame()) {
            this.frame = getMaxFrame();
        } else {
            if (isReversed() || getFrame() != getMaxFrame()) {
                return;
            }
            this.frame = getMinFrame();
        }
    }

    @Override // android.animation.ValueAnimator, android.animation.Animator
    public void cancel() {
        notifyCancel();
        removeFrameCallback();
    }

    private boolean isReversed() {
        return getSpeed() < 0.0f;
    }

    public float getMinFrame() {
        if (this.composition == null) {
            return 0.0f;
        }
        return this.minFrame == -2.1474836E9f ? this.composition.getStartFrame() : this.minFrame;
    }

    public float getMaxFrame() {
        if (this.composition == null) {
            return 0.0f;
        }
        return this.maxFrame == 2.1474836E9f ? this.composition.getEndFrame() : this.maxFrame;
    }

    protected void postFrameCallback() {
        removeFrameCallback();
        Choreographer.getInstance().postFrameCallback(this);
        this.isRunning = true;
    }

    protected void removeFrameCallback() {
        Choreographer.getInstance().removeFrameCallback(this);
        this.isRunning = false;
    }

    private void verifyFrame() {
        if (this.composition == null) {
            return;
        }
        if (this.frame < this.minFrame || this.frame > this.maxFrame) {
            throw new IllegalStateException(String.format("Frame must be [%f,%f]. It is %f", Float.valueOf(this.minFrame), Float.valueOf(this.maxFrame), Float.valueOf(this.frame)));
        }
    }
}
