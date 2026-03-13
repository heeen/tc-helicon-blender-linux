package com.airbnb.lottie.parser;

import android.util.JsonReader;
import com.airbnb.lottie.model.Font;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
class FontParser {
    private FontParser() {
    }

    static Font parse(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        String strNextString = null;
        String strNextString2 = null;
        float fNextDouble = 0.0f;
        String strNextString3 = null;
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            byte b = -1;
            int iHashCode = strNextName.hashCode();
            if (iHashCode != -1866931350) {
                if (iHashCode != -1408684838) {
                    if (iHashCode != -1294566165) {
                        if (iHashCode == 96619537 && strNextName.equals("fName")) {
                            b = 1;
                        }
                    } else if (strNextName.equals("fStyle")) {
                        b = 2;
                    }
                } else if (strNextName.equals("ascent")) {
                    b = 3;
                }
            } else if (strNextName.equals("fFamily")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    strNextString = jsonReader.nextString();
                    break;
                case 1:
                    strNextString3 = jsonReader.nextString();
                    break;
                case 2:
                    strNextString2 = jsonReader.nextString();
                    break;
                case 3:
                    fNextDouble = (float) jsonReader.nextDouble();
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        jsonReader.endObject();
        return new Font(strNextString, strNextString3, strNextString2, fNextDouble);
    }
}
