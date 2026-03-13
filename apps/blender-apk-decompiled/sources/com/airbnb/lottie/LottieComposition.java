package com.airbnb.lottie;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.RestrictTo;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.SparseArrayCompat;
import android.util.JsonReader;
import android.util.Log;
import com.airbnb.lottie.model.Font;
import com.airbnb.lottie.model.FontCharacter;
import com.airbnb.lottie.model.layer.Layer;
import com.airbnb.lottie.parser.AsyncCompositionLoader;
import com.airbnb.lottie.parser.LottieCompositionParser;
import com.airbnb.lottie.utils.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class LottieComposition {
    private Rect bounds;
    private SparseArrayCompat<FontCharacter> characters;
    private float endFrame;
    private Map<String, Font> fonts;
    private float frameRate;
    private Map<String, LottieImageAsset> images;
    private LongSparseArray<Layer> layerMap;
    private List<Layer> layers;
    private Map<String, List<Layer>> precomps;
    private float startFrame;
    private final PerformanceTracker performanceTracker = new PerformanceTracker();
    private final HashSet<String> warnings = new HashSet<>();

    public void init(Rect rect, float f, float f2, float f3, List<Layer> list, LongSparseArray<Layer> longSparseArray, Map<String, List<Layer>> map, Map<String, LottieImageAsset> map2, SparseArrayCompat<FontCharacter> sparseArrayCompat, Map<String, Font> map3) {
        this.bounds = rect;
        this.startFrame = f;
        this.endFrame = f2;
        this.frameRate = f3;
        this.layers = list;
        this.layerMap = longSparseArray;
        this.precomps = map;
        this.images = map2;
        this.characters = sparseArrayCompat;
        this.fonts = map3;
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public void addWarning(String str) {
        Log.w(L.TAG, str);
        this.warnings.add(str);
    }

    public ArrayList<String> getWarnings() {
        return new ArrayList<>(Arrays.asList(this.warnings.toArray(new String[this.warnings.size()])));
    }

    public void setPerformanceTrackingEnabled(boolean z) {
        this.performanceTracker.setEnabled(z);
    }

    public PerformanceTracker getPerformanceTracker() {
        return this.performanceTracker;
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public Layer layerModelForId(long j) {
        return this.layerMap.get(j);
    }

    public Rect getBounds() {
        return this.bounds;
    }

    public float getDuration() {
        return (long) ((getDurationFrames() / this.frameRate) * 1000.0f);
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public float getStartFrame() {
        return this.startFrame;
    }

    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public float getEndFrame() {
        return this.endFrame;
    }

    public float getFrameRate() {
        return this.frameRate;
    }

    public List<Layer> getLayers() {
        return this.layers;
    }

    @Nullable
    @RestrictTo({RestrictTo.Scope.LIBRARY})
    public List<Layer> getPrecomps(String str) {
        return this.precomps.get(str);
    }

    public SparseArrayCompat<FontCharacter> getCharacters() {
        return this.characters;
    }

    public Map<String, Font> getFonts() {
        return this.fonts;
    }

    public boolean hasImages() {
        return !this.images.isEmpty();
    }

    public Map<String, LottieImageAsset> getImages() {
        return this.images;
    }

    public float getDurationFrames() {
        return this.endFrame - this.startFrame;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("LottieComposition:\n");
        Iterator<Layer> it = this.layers.iterator();
        while (it.hasNext()) {
            sb.append(it.next().toString("\t"));
        }
        return sb.toString();
    }

    public static class Factory {
        private Factory() {
        }

        public static Cancellable fromAssetFileName(Context context, String str, OnCompositionLoadedListener onCompositionLoadedListener) {
            try {
                return fromInputStream(context.getAssets().open(str), onCompositionLoadedListener);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to find file " + str, e);
            }
        }

        public static Cancellable fromRawFile(Context context, @RawRes int i, OnCompositionLoadedListener onCompositionLoadedListener) {
            return fromInputStream(context.getResources().openRawResource(i), onCompositionLoadedListener);
        }

        public static Cancellable fromInputStream(InputStream inputStream, OnCompositionLoadedListener onCompositionLoadedListener) {
            return fromJsonReader(new JsonReader(new InputStreamReader(inputStream)), onCompositionLoadedListener);
        }

        public static Cancellable fromJsonString(String str, OnCompositionLoadedListener onCompositionLoadedListener) {
            return fromJsonReader(new JsonReader(new StringReader(str)), onCompositionLoadedListener);
        }

        public static Cancellable fromJsonReader(JsonReader jsonReader, OnCompositionLoadedListener onCompositionLoadedListener) {
            AsyncCompositionLoader asyncCompositionLoader = new AsyncCompositionLoader(onCompositionLoadedListener);
            asyncCompositionLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, jsonReader);
            return asyncCompositionLoader;
        }

        @Nullable
        public static LottieComposition fromFileSync(Context context, String str) {
            try {
                return fromInputStreamSync(context.getAssets().open(str));
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to open asset " + str, e);
            }
        }

        @Nullable
        public static LottieComposition fromInputStreamSync(InputStream inputStream) {
            return fromInputStreamSync(inputStream, true);
        }

        @Nullable
        public static LottieComposition fromInputStreamSync(InputStream inputStream, boolean z) {
            try {
                try {
                    return fromJsonSync(new JsonReader(new InputStreamReader(inputStream)));
                } catch (IOException e) {
                    throw new IllegalArgumentException("Unable to parse composition.", e);
                }
            } finally {
                if (z) {
                    Utils.closeQuietly(inputStream);
                }
            }
        }

        @Deprecated
        public static LottieComposition fromJsonSync(Resources resources, JSONObject jSONObject) {
            return fromJsonSync(jSONObject.toString());
        }

        public static LottieComposition fromJsonSync(String str) {
            try {
                return fromJsonSync(new JsonReader(new StringReader(str)));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public static LottieComposition fromJsonSync(JsonReader jsonReader) throws IOException {
            return LottieCompositionParser.parse(jsonReader);
        }
    }
}
