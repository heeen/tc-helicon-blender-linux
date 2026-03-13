package com.airbnb.lottie;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.JsonReader;
import android.util.SparseArray;
import android.view.View;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.value.LottieFrameInfo;
import com.airbnb.lottie.value.LottieValueCallback;
import com.airbnb.lottie.value.SimpleLottieValueCallback;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class LottieAnimationView extends AppCompatImageView {
    private String animationName;

    @RawRes
    private int animationResId;
    private boolean autoPlay;

    @Nullable
    private LottieComposition composition;

    @Nullable
    private Cancellable compositionLoader;
    private CacheStrategy defaultCacheStrategy;
    private final OnCompositionLoadedListener loadedListener;
    private final LottieDrawable lottieDrawable;
    private boolean useHardwareLayer;
    private boolean wasAnimatingWhenDetached;
    public static final CacheStrategy DEFAULT_CACHE_STRATEGY = CacheStrategy.Weak;
    private static final String TAG = LottieAnimationView.class.getSimpleName();
    private static final SparseArray<LottieComposition> RAW_RES_STRONG_REF_CACHE = new SparseArray<>();
    private static final SparseArray<WeakReference<LottieComposition>> RAW_RES_WEAK_REF_CACHE = new SparseArray<>();
    private static final Map<String, LottieComposition> ASSET_STRONG_REF_CACHE = new HashMap();
    private static final Map<String, WeakReference<LottieComposition>> ASSET_WEAK_REF_CACHE = new HashMap();

    public enum CacheStrategy {
        None,
        Weak,
        Strong
    }

    public LottieAnimationView(Context context) {
        super(context);
        this.loadedListener = new OnCompositionLoadedListener() { // from class: com.airbnb.lottie.LottieAnimationView.1
            @Override // com.airbnb.lottie.OnCompositionLoadedListener
            public void onCompositionLoaded(@Nullable LottieComposition lottieComposition) {
                if (lottieComposition != null) {
                    LottieAnimationView.this.setComposition(lottieComposition);
                }
                LottieAnimationView.this.compositionLoader = null;
            }
        };
        this.lottieDrawable = new LottieDrawable();
        this.wasAnimatingWhenDetached = false;
        this.autoPlay = false;
        this.useHardwareLayer = false;
        init(null);
    }

    public LottieAnimationView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.loadedListener = new OnCompositionLoadedListener() { // from class: com.airbnb.lottie.LottieAnimationView.1
            @Override // com.airbnb.lottie.OnCompositionLoadedListener
            public void onCompositionLoaded(@Nullable LottieComposition lottieComposition) {
                if (lottieComposition != null) {
                    LottieAnimationView.this.setComposition(lottieComposition);
                }
                LottieAnimationView.this.compositionLoader = null;
            }
        };
        this.lottieDrawable = new LottieDrawable();
        this.wasAnimatingWhenDetached = false;
        this.autoPlay = false;
        this.useHardwareLayer = false;
        init(attributeSet);
    }

    public LottieAnimationView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.loadedListener = new OnCompositionLoadedListener() { // from class: com.airbnb.lottie.LottieAnimationView.1
            @Override // com.airbnb.lottie.OnCompositionLoadedListener
            public void onCompositionLoaded(@Nullable LottieComposition lottieComposition) {
                if (lottieComposition != null) {
                    LottieAnimationView.this.setComposition(lottieComposition);
                }
                LottieAnimationView.this.compositionLoader = null;
            }
        };
        this.lottieDrawable = new LottieDrawable();
        this.wasAnimatingWhenDetached = false;
        this.autoPlay = false;
        this.useHardwareLayer = false;
        init(attributeSet);
    }

    private void init(@Nullable AttributeSet attributeSet) {
        String string;
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.LottieAnimationView);
        this.defaultCacheStrategy = CacheStrategy.values()[typedArrayObtainStyledAttributes.getInt(R.styleable.LottieAnimationView_lottie_cacheStrategy, DEFAULT_CACHE_STRATEGY.ordinal())];
        if (!isInEditMode()) {
            boolean zHasValue = typedArrayObtainStyledAttributes.hasValue(R.styleable.LottieAnimationView_lottie_rawRes);
            boolean zHasValue2 = typedArrayObtainStyledAttributes.hasValue(R.styleable.LottieAnimationView_lottie_fileName);
            if (zHasValue && zHasValue2) {
                throw new IllegalArgumentException("lottie_rawRes and lottie_fileName cannot be used at the same time. Please use use only one at once.");
            }
            if (zHasValue) {
                int resourceId = typedArrayObtainStyledAttributes.getResourceId(R.styleable.LottieAnimationView_lottie_rawRes, 0);
                if (resourceId != 0) {
                    setAnimation(resourceId);
                }
            } else if (zHasValue2 && (string = typedArrayObtainStyledAttributes.getString(R.styleable.LottieAnimationView_lottie_fileName)) != null) {
                setAnimation(string);
            }
        }
        if (typedArrayObtainStyledAttributes.getBoolean(R.styleable.LottieAnimationView_lottie_autoPlay, false)) {
            this.lottieDrawable.playAnimation();
            this.autoPlay = true;
        }
        if (typedArrayObtainStyledAttributes.getBoolean(R.styleable.LottieAnimationView_lottie_loop, false)) {
            this.lottieDrawable.setRepeatCount(-1);
        }
        if (typedArrayObtainStyledAttributes.hasValue(R.styleable.LottieAnimationView_lottie_repeatMode)) {
            setRepeatMode(typedArrayObtainStyledAttributes.getInt(R.styleable.LottieAnimationView_lottie_repeatMode, 1));
        }
        if (typedArrayObtainStyledAttributes.hasValue(R.styleable.LottieAnimationView_lottie_repeatCount)) {
            setRepeatCount(typedArrayObtainStyledAttributes.getInt(R.styleable.LottieAnimationView_lottie_repeatCount, -1));
        }
        setImageAssetsFolder(typedArrayObtainStyledAttributes.getString(R.styleable.LottieAnimationView_lottie_imageAssetsFolder));
        setProgress(typedArrayObtainStyledAttributes.getFloat(R.styleable.LottieAnimationView_lottie_progress, 0.0f));
        enableMergePathsForKitKatAndAbove(typedArrayObtainStyledAttributes.getBoolean(R.styleable.LottieAnimationView_lottie_enableMergePathsForKitKatAndAbove, false));
        if (typedArrayObtainStyledAttributes.hasValue(R.styleable.LottieAnimationView_lottie_colorFilter)) {
            addValueCallback(new KeyPath("**"), LottieProperty.COLOR_FILTER, (LottieValueCallback<ColorFilter>) new LottieValueCallback(new SimpleColorFilter(typedArrayObtainStyledAttributes.getColor(R.styleable.LottieAnimationView_lottie_colorFilter, 0))));
        }
        if (typedArrayObtainStyledAttributes.hasValue(R.styleable.LottieAnimationView_lottie_scale)) {
            this.lottieDrawable.setScale(typedArrayObtainStyledAttributes.getFloat(R.styleable.LottieAnimationView_lottie_scale, 1.0f));
        }
        typedArrayObtainStyledAttributes.recycle();
        enableOrDisableHardwareLayer();
    }

    @Override // android.support.v7.widget.AppCompatImageView, android.widget.ImageView
    public void setImageResource(int i) {
        recycleBitmaps();
        cancelLoaderTask();
        super.setImageResource(i);
    }

    @Override // android.support.v7.widget.AppCompatImageView, android.widget.ImageView
    public void setImageDrawable(Drawable drawable) {
        setImageDrawable(drawable, true);
    }

    private void setImageDrawable(Drawable drawable, boolean z) {
        if (z && drawable != this.lottieDrawable) {
            recycleBitmaps();
        }
        cancelLoaderTask();
        super.setImageDrawable(drawable);
    }

    @Override // android.support.v7.widget.AppCompatImageView, android.widget.ImageView
    public void setImageBitmap(Bitmap bitmap) {
        recycleBitmaps();
        cancelLoaderTask();
        super.setImageBitmap(bitmap);
    }

    @Override // android.widget.ImageView, android.view.View, android.graphics.drawable.Drawable.Callback
    public void invalidateDrawable(@NonNull Drawable drawable) {
        if (getDrawable() == this.lottieDrawable) {
            super.invalidateDrawable(this.lottieDrawable);
        } else {
            super.invalidateDrawable(drawable);
        }
    }

    @Override // android.view.View
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.animationName = this.animationName;
        savedState.animationResId = this.animationResId;
        savedState.progress = this.lottieDrawable.getProgress();
        savedState.isAnimating = this.lottieDrawable.isAnimating();
        savedState.imageAssetsFolder = this.lottieDrawable.getImageAssetsFolder();
        savedState.repeatMode = this.lottieDrawable.getRepeatMode();
        savedState.repeatCount = this.lottieDrawable.getRepeatCount();
        return savedState;
    }

    @Override // android.view.View
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedState)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.animationName = savedState.animationName;
        if (!TextUtils.isEmpty(this.animationName)) {
            setAnimation(this.animationName);
        }
        this.animationResId = savedState.animationResId;
        if (this.animationResId != 0) {
            setAnimation(this.animationResId);
        }
        setProgress(savedState.progress);
        if (savedState.isAnimating) {
            playAnimation();
        }
        this.lottieDrawable.setImagesAssetsFolder(savedState.imageAssetsFolder);
        setRepeatMode(savedState.repeatMode);
        setRepeatCount(savedState.repeatCount);
    }

    @Override // android.widget.ImageView, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.autoPlay && this.wasAnimatingWhenDetached) {
            playAnimation();
        }
    }

    @Override // android.widget.ImageView, android.view.View
    protected void onDetachedFromWindow() {
        if (isAnimating()) {
            cancelAnimation();
            this.wasAnimatingWhenDetached = true;
        }
        recycleBitmaps();
        super.onDetachedFromWindow();
    }

    @VisibleForTesting
    void recycleBitmaps() {
        if (this.lottieDrawable != null) {
            this.lottieDrawable.recycleBitmaps();
        }
    }

    public void enableMergePathsForKitKatAndAbove(boolean z) {
        this.lottieDrawable.enableMergePathsForKitKatAndAbove(z);
    }

    public boolean isMergePathsEnabledForKitKatAndAbove() {
        return this.lottieDrawable.isMergePathsEnabledForKitKatAndAbove();
    }

    @Deprecated
    public void useExperimentalHardwareAcceleration() {
        useHardwareAcceleration(true);
    }

    @Deprecated
    public void useExperimentalHardwareAcceleration(boolean z) {
        useHardwareAcceleration(z);
    }

    public void useHardwareAcceleration() {
        useHardwareAcceleration(true);
    }

    public void useHardwareAcceleration(boolean z) {
        this.useHardwareLayer = z;
        enableOrDisableHardwareLayer();
    }

    public boolean getUseHardwareAcceleration() {
        return this.useHardwareLayer;
    }

    public void setAnimation(@RawRes int i) {
        setAnimation(i, this.defaultCacheStrategy);
    }

    public void setAnimation(@RawRes final int i, final CacheStrategy cacheStrategy) {
        this.animationResId = i;
        this.animationName = null;
        if (RAW_RES_WEAK_REF_CACHE.indexOfKey(i) > 0) {
            LottieComposition lottieComposition = RAW_RES_WEAK_REF_CACHE.get(i).get();
            if (lottieComposition != null) {
                setComposition(lottieComposition);
                return;
            }
        } else if (RAW_RES_STRONG_REF_CACHE.indexOfKey(i) > 0) {
            setComposition(RAW_RES_STRONG_REF_CACHE.get(i));
            return;
        }
        clearComposition();
        cancelLoaderTask();
        this.compositionLoader = LottieComposition.Factory.fromRawFile(getContext(), i, new OnCompositionLoadedListener() { // from class: com.airbnb.lottie.LottieAnimationView.2
            @Override // com.airbnb.lottie.OnCompositionLoadedListener
            public void onCompositionLoaded(LottieComposition lottieComposition2) {
                if (cacheStrategy == CacheStrategy.Strong) {
                    LottieAnimationView.RAW_RES_STRONG_REF_CACHE.put(i, lottieComposition2);
                } else if (cacheStrategy == CacheStrategy.Weak) {
                    LottieAnimationView.RAW_RES_WEAK_REF_CACHE.put(i, new WeakReference(lottieComposition2));
                }
                LottieAnimationView.this.setComposition(lottieComposition2);
            }
        });
    }

    public void setAnimation(String str) {
        setAnimation(str, this.defaultCacheStrategy);
    }

    public void setAnimation(final String str, final CacheStrategy cacheStrategy) {
        this.animationName = str;
        this.animationResId = 0;
        if (ASSET_WEAK_REF_CACHE.containsKey(str)) {
            LottieComposition lottieComposition = ASSET_WEAK_REF_CACHE.get(str).get();
            if (lottieComposition != null) {
                setComposition(lottieComposition);
                return;
            }
        } else if (ASSET_STRONG_REF_CACHE.containsKey(str)) {
            setComposition(ASSET_STRONG_REF_CACHE.get(str));
            return;
        }
        clearComposition();
        cancelLoaderTask();
        this.compositionLoader = LottieComposition.Factory.fromAssetFileName(getContext(), str, new OnCompositionLoadedListener() { // from class: com.airbnb.lottie.LottieAnimationView.3
            @Override // com.airbnb.lottie.OnCompositionLoadedListener
            public void onCompositionLoaded(LottieComposition lottieComposition2) {
                if (cacheStrategy == CacheStrategy.Strong) {
                    LottieAnimationView.ASSET_STRONG_REF_CACHE.put(str, lottieComposition2);
                } else if (cacheStrategy == CacheStrategy.Weak) {
                    LottieAnimationView.ASSET_WEAK_REF_CACHE.put(str, new WeakReference(lottieComposition2));
                }
                LottieAnimationView.this.setComposition(lottieComposition2);
            }
        });
    }

    @Deprecated
    public void setAnimation(JSONObject jSONObject) {
        setAnimation(new JsonReader(new StringReader(jSONObject.toString())));
    }

    public void setAnimationFromJson(String str) {
        setAnimation(new JsonReader(new StringReader(str)));
    }

    public void setAnimation(JsonReader jsonReader) {
        clearComposition();
        cancelLoaderTask();
        this.compositionLoader = LottieComposition.Factory.fromJsonReader(jsonReader, this.loadedListener);
    }

    private void cancelLoaderTask() {
        if (this.compositionLoader != null) {
            this.compositionLoader.cancel();
            this.compositionLoader = null;
        }
    }

    public void setComposition(@NonNull LottieComposition lottieComposition) {
        this.lottieDrawable.setCallback(this);
        this.composition = lottieComposition;
        boolean composition = this.lottieDrawable.setComposition(lottieComposition);
        enableOrDisableHardwareLayer();
        if (getDrawable() != this.lottieDrawable || composition) {
            setImageDrawable(null);
            setImageDrawable(this.lottieDrawable);
            requestLayout();
        }
    }

    @Nullable
    public LottieComposition getComposition() {
        return this.composition;
    }

    public boolean hasMasks() {
        return this.lottieDrawable.hasMasks();
    }

    public boolean hasMatte() {
        return this.lottieDrawable.hasMatte();
    }

    public void playAnimation() {
        this.lottieDrawable.playAnimation();
        enableOrDisableHardwareLayer();
    }

    public void resumeAnimation() {
        this.lottieDrawable.resumeAnimation();
        enableOrDisableHardwareLayer();
    }

    public void setMinFrame(int i) {
        this.lottieDrawable.setMinFrame(i);
    }

    public float getMinFrame() {
        return this.lottieDrawable.getMinFrame();
    }

    public void setMinProgress(float f) {
        this.lottieDrawable.setMinProgress(f);
    }

    public void setMaxFrame(int i) {
        this.lottieDrawable.setMaxFrame(i);
    }

    public float getMaxFrame() {
        return this.lottieDrawable.getMaxFrame();
    }

    public void setMaxProgress(@FloatRange(from = 0.0d, to = 1.0d) float f) {
        this.lottieDrawable.setMaxProgress(f);
    }

    public void setMinAndMaxFrame(int i, int i2) {
        this.lottieDrawable.setMinAndMaxFrame(i, i2);
    }

    public void setMinAndMaxProgress(@FloatRange(from = 0.0d, to = 1.0d) float f, @FloatRange(from = 0.0d, to = 1.0d) float f2) {
        this.lottieDrawable.setMinAndMaxProgress(f, f2);
    }

    public void reverseAnimationSpeed() {
        this.lottieDrawable.reverseAnimationSpeed();
    }

    public void setSpeed(float f) {
        this.lottieDrawable.setSpeed(f);
    }

    public float getSpeed() {
        return this.lottieDrawable.getSpeed();
    }

    public void addAnimatorUpdateListener(ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        this.lottieDrawable.addAnimatorUpdateListener(animatorUpdateListener);
    }

    public void removeUpdateListener(ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        this.lottieDrawable.removeAnimatorUpdateListener(animatorUpdateListener);
    }

    public void removeAllUpdateListeners() {
        this.lottieDrawable.removeAllUpdateListeners();
    }

    public void addAnimatorListener(Animator.AnimatorListener animatorListener) {
        this.lottieDrawable.addAnimatorListener(animatorListener);
    }

    public void removeAnimatorListener(Animator.AnimatorListener animatorListener) {
        this.lottieDrawable.removeAnimatorListener(animatorListener);
    }

    public void removeAllAnimatorListeners() {
        this.lottieDrawable.removeAllAnimatorListeners();
    }

    @Deprecated
    public void loop(boolean z) {
        this.lottieDrawable.setRepeatCount(z ? -1 : 0);
    }

    public void setRepeatMode(int i) {
        this.lottieDrawable.setRepeatMode(i);
    }

    public int getRepeatMode() {
        return this.lottieDrawable.getRepeatMode();
    }

    public void setRepeatCount(int i) {
        this.lottieDrawable.setRepeatCount(i);
    }

    public int getRepeatCount() {
        return this.lottieDrawable.getRepeatCount();
    }

    public boolean isAnimating() {
        return this.lottieDrawable.isAnimating();
    }

    public void setImageAssetsFolder(String str) {
        this.lottieDrawable.setImagesAssetsFolder(str);
    }

    @Nullable
    public String getImageAssetsFolder() {
        return this.lottieDrawable.getImageAssetsFolder();
    }

    @Nullable
    public Bitmap updateBitmap(String str, @Nullable Bitmap bitmap) {
        return this.lottieDrawable.updateBitmap(str, bitmap);
    }

    public void setImageAssetDelegate(ImageAssetDelegate imageAssetDelegate) {
        this.lottieDrawable.setImageAssetDelegate(imageAssetDelegate);
    }

    public void setFontAssetDelegate(FontAssetDelegate fontAssetDelegate) {
        this.lottieDrawable.setFontAssetDelegate(fontAssetDelegate);
    }

    public void setTextDelegate(TextDelegate textDelegate) {
        this.lottieDrawable.setTextDelegate(textDelegate);
    }

    public List<KeyPath> resolveKeyPath(KeyPath keyPath) {
        return this.lottieDrawable.resolveKeyPath(keyPath);
    }

    public <T> void addValueCallback(KeyPath keyPath, T t, LottieValueCallback<T> lottieValueCallback) {
        this.lottieDrawable.addValueCallback(keyPath, t, lottieValueCallback);
    }

    public <T> void addValueCallback(KeyPath keyPath, T t, final SimpleLottieValueCallback<T> simpleLottieValueCallback) {
        this.lottieDrawable.addValueCallback(keyPath, t, new LottieValueCallback<T>() { // from class: com.airbnb.lottie.LottieAnimationView.4
            @Override // com.airbnb.lottie.value.LottieValueCallback
            public T getValue(LottieFrameInfo<T> lottieFrameInfo) {
                return (T) simpleLottieValueCallback.getValue(lottieFrameInfo);
            }
        });
    }

    public void setScale(float f) {
        this.lottieDrawable.setScale(f);
        if (getDrawable() == this.lottieDrawable) {
            setImageDrawable(null, false);
            setImageDrawable(this.lottieDrawable, false);
        }
    }

    public float getScale() {
        return this.lottieDrawable.getScale();
    }

    public void cancelAnimation() {
        this.lottieDrawable.cancelAnimation();
        enableOrDisableHardwareLayer();
    }

    public void pauseAnimation() {
        this.lottieDrawable.pauseAnimation();
        enableOrDisableHardwareLayer();
    }

    public void setFrame(int i) {
        this.lottieDrawable.setFrame(i);
    }

    public int getFrame() {
        return this.lottieDrawable.getFrame();
    }

    public void setProgress(@FloatRange(from = 0.0d, to = 1.0d) float f) {
        this.lottieDrawable.setProgress(f);
    }

    @FloatRange(from = 0.0d, to = 1.0d)
    public float getProgress() {
        return this.lottieDrawable.getProgress();
    }

    public long getDuration() {
        if (this.composition != null) {
            return (long) this.composition.getDuration();
        }
        return 0L;
    }

    public void setPerformanceTrackingEnabled(boolean z) {
        this.lottieDrawable.setPerformanceTrackingEnabled(z);
    }

    @Nullable
    public PerformanceTracker getPerformanceTracker() {
        return this.lottieDrawable.getPerformanceTracker();
    }

    private void clearComposition() {
        this.composition = null;
        this.lottieDrawable.clearComposition();
    }

    private void enableOrDisableHardwareLayer() {
        setLayerType(this.useHardwareLayer && this.lottieDrawable.isAnimating() ? 2 : 1, null);
    }

    private static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() { // from class: com.airbnb.lottie.LottieAnimationView.SavedState.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        String animationName;
        int animationResId;
        String imageAssetsFolder;
        boolean isAnimating;
        float progress;
        int repeatCount;
        int repeatMode;

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.animationName = parcel.readString();
            this.progress = parcel.readFloat();
            this.isAnimating = parcel.readInt() == 1;
            this.imageAssetsFolder = parcel.readString();
            this.repeatMode = parcel.readInt();
            this.repeatCount = parcel.readInt();
        }

        @Override // android.view.View.BaseSavedState, android.view.AbsSavedState, android.os.Parcelable
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeString(this.animationName);
            parcel.writeFloat(this.progress);
            parcel.writeInt(this.isAnimating ? 1 : 0);
            parcel.writeString(this.imageAssetsFolder);
            parcel.writeInt(this.repeatMode);
            parcel.writeInt(this.repeatCount);
        }
    }
}
