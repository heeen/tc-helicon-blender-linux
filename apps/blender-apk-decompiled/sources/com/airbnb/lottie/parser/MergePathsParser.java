package com.airbnb.lottie.parser;

import android.util.JsonReader;
import com.airbnb.lottie.model.content.MergePaths;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
class MergePathsParser {
    private MergePathsParser() {
    }

    static MergePaths parse(JsonReader jsonReader) throws IOException {
        String strNextString = null;
        MergePaths.MergePathsMode mergePathsModeForId = null;
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            byte b = -1;
            int iHashCode = strNextName.hashCode();
            if (iHashCode != 3488) {
                if (iHashCode == 3519 && strNextName.equals("nm")) {
                    b = 0;
                }
            } else if (strNextName.equals("mm")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    strNextString = jsonReader.nextString();
                    break;
                case 1:
                    mergePathsModeForId = MergePaths.MergePathsMode.forId(jsonReader.nextInt());
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        return new MergePaths(strNextString, mergePathsModeForId);
    }
}
