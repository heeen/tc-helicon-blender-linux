package com.airbnb.lottie;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;
import android.view.View;
import com.airbnb.lottie.manager.FontAssetManager;
import com.airbnb.lottie.manager.ImageAssetManager;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.model.layer.CompositionLayer;
import com.airbnb.lottie.parser.LayerParser;
import com.airbnb.lottie.utils.LottieValueAnimator;
import com.airbnb.lottie.utils.MiscUtils;
import com.airbnb.lottie.value.LottieFrameInfo;
import com.airbnb.lottie.value.LottieValueCallback;
import com.airbnb.lottie.value.SimpleLottieValueCallback;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public class LottieDrawable extends Drawable implements Drawable.Callback, Animatable {
    public static final int INFINITE = -1;
    public static final int RESTART = 1;
    public static final int REVERSE = 2;
    private static final String TAG = "LottieDrawable";
    private LottieComposition composition;

    @Nullable
    private CompositionLayer compositionLayer;
    private boolean enableMergePaths;

    @Nullable
    FontAssetDelegate fontAssetDelegate;

    @Nullable
    private FontAssetManager fontAssetManager;

    @Nullable
    private ImageAssetDelegate imageAssetDelegate;

    @Nullable
    private ImageAssetManager imageAssetManager;

    @Nullable
    private String imageAssetsFolder;
    private boolean performanceTrackingEnabled;

    @Nullable
    TextDelegate textDelegate;
    private final Matrix matrix = new Matrix();
    private final LottieValueAnimator animator = new LottieValueAnimator();
    private float scale = 1.0f;
    private final Set<ColorFilterData> colorFilterData = new HashSet();
    private final ArrayList<LazyCompositionTask> lazyCompositionTasks = new ArrayList<>();
    private int alpha = 255;

    private interface LazyCompositionTask {
        void run(LottieComposition lottieComposition);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RepeatMode {
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -3;
    }

    public LottieDrawable() {
        this.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.airbnb.lottie.LottieDrawable.1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (LottieDrawable.this.compositionLayer != null) {
                    LottieDrawable.this.compositionLayer.setProgress(LottieDrawable.this.animator.getAnimatedValueAbsolute());
                }
            }
        });
    }

    public boolean hasMasks() {
        return this.compositionLayer != null && this.compositionLayer.hasMasks();
    }

    public boolean hasMatte() {
        return this.compositionLayer != null && this.compositionLayer.hasMatte();
    }

    public boolean enableMergePathsForKitKatAndAbove() {
        return this.enableMergePaths;
    }

    public void enableMergePathsForKitKatAndAbove(boolean z) {
        if (Build.VERSION.SDK_INT < 19) {
            Log.w(TAG, "Merge paths are not supported pre-Kit Kat.");
            return;
        }
        this.enableMergePaths = z;
        if (this.composition != null) {
            buildCompositionLayer();
        }
    }

    public boolean isMergePathsEnabledForKitKatAndAbove() {
        return this.enableMergePaths;
    }

    public void setImagesAssetsFolder(@Nullable String str) {
        this.imageAssetsFolder = str;
    }

    @Nullable
    public String getImageAssetsFolder() {
        return this.imageAssetsFolder;
    }

    public void recycleBitmaps() {
        if (this.imageAssetManager != null) {
            this.imageAssetManager.recycleBitmaps();
        }
    }

    public boolean setComposition(LottieComposition lottieComposition) {
        if (this.composition == lottieComposition) {
            return false;
        }
        clearComposition();
        this.composition = lottieComposition;
        buildCompositionLayer();
        this.animator.setComposition(lottieComposition);
        setProgress(this.animator.getAnimatedFraction());
        setScale(this.scale);
        updateBounds();
        Iterator it = new ArrayList(this.lazyCompositionTasks).iterator();
        while (it.hasNext()) {
            ((LazyCompositionTask) it.next()).run(lottieComposition);
            it.remove();
        }
        this.lazyCompositionTasks.clear();
        lottieComposition.setPerformanceTrackingEnabled(this.performanceTrackingEnabled);
        return true;
    }

    public void setPerformanceTrackingEnabled(boolean z) {
        this.performanceTrackingEnabled = z;
        if (this.composition != null) {
            this.composition.setPerformanceTrackingEnabled(z);
        }
    }

    @Nullable
    public PerformanceTracker getPerformanceTracker() {
        if (this.composition != null) {
            return this.composition.getPerformanceTracker();
        }
        return null;
    }

    private void buildCompositionLayer() {
        this.compositionLayer = new CompositionLayer(this, LayerParser.parse(this.composition), this.composition.getLayers(), this.composition);
    }

    public void clearComposition() {
        recycleBitmaps();
        if (this.animator.isRunning()) {
            this.animator.cancel();
        }
        this.composition = null;
        this.compositionLayer = null;
        this.imageAssetManager = null;
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public void invalidateSelf() {
        Drawable.Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(@IntRange(from = MediaDescriptionCompat.BT_FOLDER_TYPE_MIXED, to = 255) int i) {
        this.alpha = i;
    }

    @Override // android.graphics.drawable.Drawable
    public int getAlpha() {
        return this.alpha;
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        Log.w(L.TAG, "Use addColorFilter instead.");
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(@NonNull Canvas canvas) {
        float f;
        L.beginSection("Drawable#draw");
        if (this.compositionLayer == null) {
            return;
        }
        float f2 = this.scale;
        float maxScale = getMaxScale(canvas);
        if (f2 > maxScale) {
            f = this.scale / maxScale;
        } else {
            maxScale = f2;
            f = 1.0f;
        }
        if (f > 1.0f) {
            canvas.save();
            float fWidth = this.composition.getBounds().width() / 2.0f;
            float fHeight = this.composition.getBounds().height() / 2.0f;
            float f3 = fWidth * maxScale;
            float f4 = fHeight * maxScale;
            canvas.translate((getScale() * fWidth) - f3, (getScale() * fHeight) - f4);
            canvas.scale(f, f, f3, f4);
        }
        this.matrix.reset();
        this.matrix.preScale(maxScale, maxScale);
        this.compositionLayer.draw(canvas, this.matrix, this.alpha);
        L.endSection("Drawable#draw");
        if (f > 1.0f) {
            canvas.restore();
        }
    }

    @Override // android.graphics.drawable.Animatable
    public void start() {
        playAnimation();
    }

    @Override // android.graphics.drawable.Animatable
    public void stop() {
        endAnimation();
    }

    @Override // android.graphics.drawable.Animatable
    public boolean isRunning() {
        return isAnimating();
    }

    public void playAnimation() {
        if (this.compositionLayer == null) {
            this.lazyCompositionTasks.add(new LazyCompositionTask() { // from class: com.airbnb.lottie.LottieDrawable.2
                @Override // com.airbnb.lottie.LottieDrawable.LazyCompositionTask
                public void run(LottieComposition lottieComposition) {
                    LottieDrawable.this.playAnimation();
                }
            });
        } else {
            this.animator.playAnimation();
        }
    }

    public void endAnimation() {
        this.lazyCompositionTasks.clear();
        this.animator.endAnimation();
    }

    public void resumeAnimation() {
        if (this.compositionLayer == null) {
            this.lazyCompositionTasks.add(new LazyCompositionTask() { // from class: com.airbnb.lottie.LottieDrawable.3
                @Override // com.airbnb.lottie.LottieDrawable.LazyCompositionTask
                public void run(LottieComposition lottieComposition) {
                    LottieDrawable.this.resumeAnimation();
                }
            });
        } else {
            this.animator.resumeAnimation();
        }
    }

    public void setMinFrame(int i) {
        this.animator.setMinFrame(i);
    }

    public float getMinFrame() {
        return this.animator.getMinFrame();
    }

    public void setMinProgress(final float f) {
        if (this.composition == null) {
            this.lazyCompositionTasks.add(new LazyCompositionTask() { // from class: com.airbnb.lottie.LottieDrawable.4
                @Override // com.airbnb.lottie.LottieDrawable.LazyCompositionTask
                public void run(LottieComposition lottieComposition) {
                    LottieDrawable.this.setMinProgress(f);
                }
            });
        } else {
            setMinFrame((int) MiscUtils.lerp(f, this.composition.getStartFrame(), this.composition.getEndFrame()));
        }
    }

    public void setMaxFrame(int i) {
        this.animator.setMaxFrame(i);
    }

    public float getMaxFrame() {
        return this.animator.getMaxFrame();
    }

    public void setMaxProgress(@FloatRange(from = 0.0d, to = 1.0d) final float f) {
        if (this.composition == null) {
            this.lazyCompositionTasks.add(new LazyCompositionTask() { // from class: com.airbnb.lottie.LottieDrawable.5
                @Override // com.airbnb.lottie.LottieDrawable.LazyCompositionTask
                public void run(LottieComposition lottieComposition) {
                    LottieDrawable.this.setMaxProgress(f);
                }
            });
        } else {
            setMaxFrame((int) MiscUtils.lerp(f, this.composition.getStartFrame(), this.composition.getEndFrame()));
        }
    }

    public void setMinAndMaxFrame(int i, int i2) {
        this.animator.setMinAndMaxFrames(i, i2);
    }

    public void setMinAndMaxProgress(@FloatRange(from = 0.0d, to = 1.0d) final float f, @FloatRange(from = 0.0d, to = 1.0d) final float f2) {
        if (this.composition == null) {
            this.lazyCompositionTasks.add(new LazyCompositionTask() { // from class: com.airbnb.lottie.LottieDrawable.6
                @Override // com.airbnb.lottie.LottieDrawable.LazyCompositionTask
                public void run(LottieComposition lottieComposition) {
                    LottieDrawable.this.setMinAndMaxProgress(f, f2);
                }
            });
        } else {
            setMinAndMaxFrame((int) (f * this.composition.getDurationFrames()), (int) (f2 * this.composition.getDurationFrames()));
        }
    }

    public void reverseAnimationSpeed() {
        this.animator.reverseAnimationSpeed();
    }

    public void setSpeed(float f) {
        this.animator.setSpeed(f);
    }

    public float getSpeed() {
        return this.animator.getSpeed();
    }

    public void addAnimatorUpdateListener(ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        this.animator.addUpdateListener(animatorUpdateListener);
    }

    public void removeAnimatorUpdateListener(ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        this.animator.removeUpdateListener(animatorUpdateListener);
    }

    public void removeAllUpdateListeners() {
        this.animator.removeAllUpdateListeners();
    }

    public void addAnimatorListener(Animator.AnimatorListener animatorListener) {
        this.animator.addListener(animatorListener);
    }

    public void removeAnimatorListener(Animator.AnimatorListener animatorListener) {
        this.animator.removeListener(animatorListener);
    }

    public void removeAllAnimatorListeners() {
        this.animator.removeAllListeners();
    }

    public void setFrame(final int i) {
        if (this.composition == null) {
            this.lazyCompositionTasks.add(new LazyCompositionTask() { // from class: com.airbnb.lottie.LottieDrawable.7
                @Override // com.airbnb.lottie.LottieDrawable.LazyCompositionTask
                public void run(LottieComposition lottieComposition) {
                    LottieDrawable.this.setFrame(i);
                }
            });
        } else {
            this.animator.setFrame(i);
        }
    }

    public int getFrame() {
        return (int) this.animator.getFrame();
    }

    public void setProgress(@FloatRange(from = 0.0d, to = 1.0d) final float f) {
        if (this.composition == null) {
            this.lazyCompositionTasks.add(new LazyCompositionTask() { // from class: com.airbnb.lottie.LottieDrawable.8
                @Override // com.airbnb.lottie.LottieDrawable.LazyCompositionTask
                public void run(LottieComposition lottieComposition) {
                    LottieDrawable.this.setProgress(f);
                }
            });
        } else {
            setFrame((int) MiscUtils.lerp(this.composition.getStartFrame(), this.composition.getEndFrame(), f));
        }
    }

    @Deprecated
    public void loop(boolean z) {
        this.animator.setRepeatCount(z ? -1 : 0);
    }

    public void setRepeatMode(int i) {
        this.animator.setRepeatMode(i);
    }

    public int getRepeatMode() {
        return this.animator.getRepeatMode();
    }

    public void setRepeatCount(int i) {
        this.animator.setRepeatCount(i);
    }

    public int getRepeatCount() {
        return this.animator.getRepeatCount();
    }

    public boolean isLooping() {
        return this.animator.getRepeatCount() == -1;
    }

    public boolean isAnimating() {
        return this.animator.isRunning();
    }

    public void setScale(float f) {
        this.scale = f;
        updateBounds();
    }

    public void setImageAssetDelegate(ImageAssetDelegate imageAssetDelegate) {
        this.imageAssetDelegate = imageAssetDelegate;
        if (this.imageAssetManager != null) {
            this.imageAssetManager.setDelegate(imageAssetDelegate);
        }
    }

    public void setFontAssetDelegate(FontAssetDelegate fontAssetDelegate) {
        this.fontAssetDelegate = fontAssetDelegate;
        if (this.fontAssetManager != null) {
            this.fontAssetManager.setDelegate(fontAssetDelegate);
        }
    }

    public void setTextDelegate(TextDelegate textDelegate) {
        this.textDelegate = textDelegate;
    }

    @Nullable
    public TextDelegate getTextDelegate() {
        return this.textDelegate;
    }

    public boolean useTextGlyphs() {
        return this.textDelegate == null && this.composition.getCharacters().size() > 0;
    }

    public float getScale() {
        return this.scale;
    }

    public LottieComposition getComposition() {
        return this.composition;
    }

    private void updateBounds() {
        if (this.composition == null) {
            return;
        }
        float scale = getScale();
        setBounds(0, 0, (int) (this.composition.getBounds().width() * scale), (int) (this.composition.getBounds().height() * scale));
    }

    public void cancelAnimation() {
        this.lazyCompositionTasks.clear();
        this.animator.cancel();
    }

    public void pauseAnimation() {
        this.lazyCompositionTasks.clear();
        this.animator.pauseAnimation();
    }

    @FloatRange(from = 0.0d, to = 1.0d)
    public float getProgress() {
        return this.animator.getAnimatedValueAbsolute();
    }

    @Override // android.graphics.drawable.Drawable
    public int getIntrinsicWidth() {
        if (this.composition == null) {
            return -1;
        }
        return (int) (this.composition.getBounds().width() * getScale());
    }

    @Override // android.graphics.drawable.Drawable
    public int getIntrinsicHeight() {
        if (this.composition == null) {
            return -1;
        }
        return (int) (this.composition.getBounds().height() * getScale());
    }

    public List<KeyPath> resolveKeyPath(KeyPath keyPath) {
        if (this.compositionLayer == null) {
            Log.w(L.TAG, "Cannot resolve KeyPath. Composition is not set yet.");
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        this.compositionLayer.resolveKeyPath(keyPath, 0, arrayList, new KeyPath(new String[0]));
        return arrayList;
    }

    public <T> void addValueCallback(final KeyPath keyPath, final T t, final LottieValueCallback<T> lottieValueCallback) {
        if (this.compositionLayer == null) {
            this.lazyCompositionTasks.add(new LazyCompositionTask() { // from class: com.airbnb.lottie.LottieDrawable.9
                @Override // com.airbnb.lottie.LottieDrawable.LazyCompositionTask
                public void run(LottieComposition lottieComposition) {
                    LottieDrawable.this.addValueCallback(keyPath, t, (LottieValueCallback<Object>) lottieValueCallback);
                }
            });
            return;
        }
        boolean zIsEmpty = true;
        if (keyPath.getResolvedElement() != null) {
            keyPath.getResolvedElement().addValueCallback(t, lottieValueCallback);
        } else {
            List<KeyPath> listResolveKeyPath = resolveKeyPath(keyPath);
            for (int i = 0; i < listResolveKeyPath.size(); i++) {
                listResolveKeyPath.get(i).getResolvedElement().addValueCallback(t, lottieValueCallback);
            }
            zIsEmpty = true ^ listResolveKeyPath.isEmpty();
        }
        if (zIsEmpty) {
            invalidateSelf();
            if (t == LottieProperty.TIME_REMAP) {
                setProgress(getProgress());
            }
        }
    }

    public <T> void addValueCallback(KeyPath keyPath, T t, final SimpleLottieValueCallback<T> simpleLottieValueCallback) {
        addValueCallback(keyPath, t, new LottieValueCallback<T>() { // from class: com.airbnb.lottie.LottieDrawable.10
            @Override // com.airbnb.lottie.value.LottieValueCallback
            public T getValue(LottieFrameInfo<T> lottieFrameInfo) {
                return (T) simpleLottieValueCallback.getValue(lottieFrameInfo);
            }
        });
    }

    @Nullable
    public Bitmap updateBitmap(String str, @Nullable Bitmap bitmap) {
        ImageAssetManager imageAssetManager = getImageAssetManager();
        if (imageAssetManager == null) {
            Log.w(L.TAG, "Cannot update bitmap. Most likely the drawable is not added to a View which prevents Lottie from getting a Context.");
            return null;
        }
        Bitmap bitmapUpdateBitmap = imageAssetManager.updateBitmap(str, bitmap);
        invalidateSelf();
        return bitmapUpdateBitmap;
    }

    @Nullable
    public Bitmap getImageAsset(String str) {
        ImageAssetManager imageAssetManager = getImageAssetManager();
        if (imageAssetManager != null) {
            return imageAssetManager.bitmapForId(str);
        }
        return null;
    }

    private ImageAssetManager getImageAssetManager() {
        if (getCallback() == null) {
            return null;
        }
        if (this.imageAssetManager != null && !this.imageAssetManager.hasSameContext(getContext())) {
            this.imageAssetManager.recycleBitmaps();
            this.imageAssetManager = null;
        }
        if (this.imageAssetManager == null) {
            this.imageAssetManager = new ImageAssetManager(getCallback(), this.imageAssetsFolder, this.imageAssetDelegate, this.composition.getImages());
        }
        return this.imageAssetManager;
    }

    @Nullable
    public Typeface getTypeface(String str, String str2) {
        FontAssetManager fontAssetManager = getFontAssetManager();
        if (fontAssetManager != null) {
            return fontAssetManager.getTypeface(str, str2);
        }
        return null;
    }

    private FontAssetManager getFontAssetManager() {
        if (getCallback() == null) {
            return null;
        }
        if (this.fontAssetManager == null) {
            this.fontAssetManager = new FontAssetManager(getCallback(), this.fontAssetDelegate);
        }
        return this.fontAssetManager;
    }

    @Nullable
    private Context getContext() {
        Drawable.Callback callback = getCallback();
        if (callback != null && (callback instanceof View)) {
            return ((View) callback).getContext();
        }
        return null;
    }

    @Override // android.graphics.drawable.Drawable.Callback
    public void invalidateDrawable(@NonNull Drawable drawable) {
        Drawable.Callback callback = getCallback();
        if (callback == null) {
            return;
        }
        callback.invalidateDrawable(this);
    }

    @Override // android.graphics.drawable.Drawable.Callback
    public void scheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable, long j) {
        Drawable.Callback callback = getCallback();
        if (callback == null) {
            return;
        }
        callback.scheduleDrawable(this, runnable, j);
    }

    @Override // android.graphics.drawable.Drawable.Callback
    public void unscheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable) {
        Drawable.Callback callback = getCallback();
        if (callback == null) {
            return;
        }
        callback.unscheduleDrawable(this, runnable);
    }

    private float getMaxScale(@NonNull Canvas canvas) {
        return Math.min(canvas.getWidth() / this.composition.getBounds().width(), canvas.getHeight() / this.composition.getBounds().height());
    }

    private static class ColorFilterData {

        @Nullable
        final ColorFilter colorFilter;

        @Nullable
        final String contentName;
        final String layerName;

        ColorFilterData(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
            this.layerName = str;
            this.contentName = str2;
            this.colorFilter = colorFilter;
        }

        public int hashCode() {
            int iHashCode = this.layerName != null ? 527 * this.layerName.hashCode() : 17;
            return this.contentName != null ? iHashCode * 31 * this.contentName.hashCode() : iHashCode;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ColorFilterData)) {
                return false;
            }
            ColorFilterData colorFilterData = (ColorFilterData) obj;
            return hashCode() == colorFilterData.hashCode() && this.colorFilter == colorFilterData.colorFilter;
        }
    }
}
