package com.airbnb.lottie.parser;

import android.graphics.PointF;
import android.util.JsonReader;
import android.util.JsonToken;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.model.animatable.AnimatableFloatValue;
import com.airbnb.lottie.model.animatable.AnimatablePathValue;
import com.airbnb.lottie.model.animatable.AnimatableSplitDimensionPathValue;
import com.airbnb.lottie.model.animatable.AnimatableValue;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.Keyframe;
import java.io.IOException;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class AnimatablePathValueParser {
    private AnimatablePathValueParser() {
    }

    public static AnimatablePathValue parse(JsonReader jsonReader, LottieComposition lottieComposition) throws IOException {
        ArrayList arrayList = new ArrayList();
        if (jsonReader.peek() == JsonToken.BEGIN_ARRAY) {
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                arrayList.add(PathKeyframeParser.parse(jsonReader, lottieComposition));
            }
            jsonReader.endArray();
            KeyframesParser.setEndFrames(arrayList);
        } else {
            arrayList.add(new Keyframe(JsonUtils.jsonToPoint(jsonReader, Utils.dpScale())));
        }
        return new AnimatablePathValue(arrayList);
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    static AnimatableValue<PointF, PointF> parseSplitPath(JsonReader jsonReader, LottieComposition lottieComposition) throws IOException {
        byte b;
        jsonReader.beginObject();
        AnimatablePathValue animatablePathValue = null;
        AnimatableFloatValue animatableFloatValue = null;
        AnimatableFloatValue animatableFloatValue2 = null;
        boolean z = false;
        while (jsonReader.peek() != JsonToken.END_OBJECT) {
            String strNextName = jsonReader.nextName();
            int iHashCode = strNextName.hashCode();
            if (iHashCode != 107) {
                switch (iHashCode) {
                    case 120:
                        b = !strNextName.equals("x") ? (byte) -1 : (byte) 1;
                        break;
                    case 121:
                        b = !strNextName.equals("y") ? (byte) -1 : (byte) 2;
                        break;
                    default:
                        b = -1;
                        break;
                }
            } else {
                b = strNextName.equals("k") ? (byte) 0 : (byte) -1;
            }
            switch (b) {
                case 0:
                    animatablePathValue = parse(jsonReader, lottieComposition);
                    continue;
                case 1:
                    if (jsonReader.peek() == JsonToken.STRING) {
                        jsonReader.skipValue();
                    } else {
                        animatableFloatValue = AnimatableValueParser.parseFloat(jsonReader, lottieComposition);
                    }
                    break;
                case 2:
                    if (jsonReader.peek() == JsonToken.STRING) {
                        jsonReader.skipValue();
                    } else {
                        animatableFloatValue2 = AnimatableValueParser.parseFloat(jsonReader, lottieComposition);
                    }
                    break;
                default:
                    jsonReader.skipValue();
                    continue;
            }
            z = true;
        }
        jsonReader.endObject();
        if (z) {
            lottieComposition.addWarning("Lottie doesn't support expressions.");
        }
        return animatablePathValue != null ? animatablePathValue : new AnimatableSplitDimensionPathValue(animatableFloatValue, animatableFloatValue2);
    }
}
