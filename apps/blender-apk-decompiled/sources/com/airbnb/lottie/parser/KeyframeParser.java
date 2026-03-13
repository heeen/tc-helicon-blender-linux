package com.airbnb.lottie.parser;

import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.animation.PathInterpolatorCompat;
import android.util.JsonReader;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.utils.MiscUtils;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.Keyframe;
import java.io.IOException;
import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes.dex */
class KeyframeParser {
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final float MAX_CP_VALUE = 100.0f;
    private static SparseArrayCompat<WeakReference<Interpolator>> pathInterpolatorCache;

    KeyframeParser() {
    }

    private static SparseArrayCompat<WeakReference<Interpolator>> pathInterpolatorCache() {
        if (pathInterpolatorCache == null) {
            pathInterpolatorCache = new SparseArrayCompat<>();
        }
        return pathInterpolatorCache;
    }

    @Nullable
    private static WeakReference<Interpolator> getInterpolator(int i) {
        WeakReference<Interpolator> weakReference;
        synchronized (KeyframeParser.class) {
            weakReference = pathInterpolatorCache().get(i);
        }
        return weakReference;
    }

    private static void putInterpolator(int i, WeakReference<Interpolator> weakReference) {
        synchronized (KeyframeParser.class) {
            pathInterpolatorCache.put(i, weakReference);
        }
    }

    static <T> Keyframe<T> parse(JsonReader jsonReader, LottieComposition lottieComposition, float f, ValueParser<T> valueParser, boolean z) throws IOException {
        if (z) {
            return parseKeyframe(lottieComposition, jsonReader, f, valueParser);
        }
        return parseStaticValue(jsonReader, f, valueParser);
    }

    private static <T> Keyframe<T> parseKeyframe(LottieComposition lottieComposition, JsonReader jsonReader, float f, ValueParser<T> valueParser) throws IOException {
        Interpolator interpolator;
        jsonReader.beginObject();
        PointF pointFJsonToPoint = null;
        PointF pointFJsonToPoint2 = null;
        T t = null;
        T t2 = null;
        PointF pointFJsonToPoint3 = null;
        PointF pointFJsonToPoint4 = null;
        float fNextDouble = 0.0f;
        boolean z = false;
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "t":
                    fNextDouble = (float) jsonReader.nextDouble();
                    break;
                case "s":
                    t = valueParser.parse(jsonReader, f);
                    break;
                case "e":
                    t2 = valueParser.parse(jsonReader, f);
                    break;
                case "o":
                    pointFJsonToPoint = JsonUtils.jsonToPoint(jsonReader, f);
                    break;
                case "i":
                    pointFJsonToPoint2 = JsonUtils.jsonToPoint(jsonReader, f);
                    break;
                case "h":
                    z = jsonReader.nextInt() == 1;
                    break;
                case "to":
                    pointFJsonToPoint3 = JsonUtils.jsonToPoint(jsonReader, f);
                    break;
                case "ti":
                    pointFJsonToPoint4 = JsonUtils.jsonToPoint(jsonReader, f);
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        jsonReader.endObject();
        if (z) {
            interpolator = LINEAR_INTERPOLATOR;
            t2 = t;
        } else if (pointFJsonToPoint != null && pointFJsonToPoint2 != null) {
            float f2 = -f;
            pointFJsonToPoint.x = MiscUtils.clamp(pointFJsonToPoint.x, f2, f);
            pointFJsonToPoint.y = MiscUtils.clamp(pointFJsonToPoint.y, -100.0f, MAX_CP_VALUE);
            pointFJsonToPoint2.x = MiscUtils.clamp(pointFJsonToPoint2.x, f2, f);
            pointFJsonToPoint2.y = MiscUtils.clamp(pointFJsonToPoint2.y, -100.0f, MAX_CP_VALUE);
            int iHashFor = Utils.hashFor(pointFJsonToPoint.x, pointFJsonToPoint.y, pointFJsonToPoint2.x, pointFJsonToPoint2.y);
            WeakReference<Interpolator> interpolator2 = getInterpolator(iHashFor);
            Interpolator interpolatorCreate = interpolator2 != null ? interpolator2.get() : null;
            if (interpolator2 == null || interpolatorCreate == null) {
                interpolatorCreate = PathInterpolatorCompat.create(pointFJsonToPoint.x / f, pointFJsonToPoint.y / f, pointFJsonToPoint2.x / f, pointFJsonToPoint2.y / f);
                try {
                    putInterpolator(iHashFor, new WeakReference(interpolatorCreate));
                } catch (ArrayIndexOutOfBoundsException unused) {
                }
            }
            interpolator = interpolatorCreate;
        } else {
            interpolator = LINEAR_INTERPOLATOR;
        }
        Keyframe<T> keyframe = new Keyframe<>(lottieComposition, t, t2, interpolator, fNextDouble, null);
        keyframe.pathCp1 = pointFJsonToPoint3;
        keyframe.pathCp2 = pointFJsonToPoint4;
        return keyframe;
    }

    private static <T> Keyframe<T> parseStaticValue(JsonReader jsonReader, float f, ValueParser<T> valueParser) throws IOException {
        return new Keyframe<>(valueParser.parse(jsonReader, f));
    }
}
