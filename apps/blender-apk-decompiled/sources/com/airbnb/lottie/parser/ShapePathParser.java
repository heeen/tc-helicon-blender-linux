package com.airbnb.lottie.parser;

/* JADX INFO: loaded from: classes.dex */
class ShapePathParser {
    private ShapePathParser() {
    }

    /* JADX WARN: Removed duplicated region for block: B:21:0x003f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    static com.airbnb.lottie.model.content.ShapePath parse(android.util.JsonReader r8, com.airbnb.lottie.LottieComposition r9) throws java.io.IOException {
        /*
            r0 = 0
            r1 = 0
            r2 = r0
            r3 = r1
        L4:
            boolean r4 = r8.hasNext()
            if (r4 == 0) goto L56
            java.lang.String r4 = r8.nextName()
            r5 = -1
            int r6 = r4.hashCode()
            r7 = 3432(0xd68, float:4.809E-42)
            if (r6 == r7) goto L35
            r7 = 3519(0xdbf, float:4.931E-42)
            if (r6 == r7) goto L2b
            r7 = 104415(0x197df, float:1.46317E-40)
            if (r6 == r7) goto L21
            goto L3f
        L21:
            java.lang.String r6 = "ind"
            boolean r4 = r4.equals(r6)
            if (r4 == 0) goto L3f
            r4 = 1
            goto L40
        L2b:
            java.lang.String r6 = "nm"
            boolean r4 = r4.equals(r6)
            if (r4 == 0) goto L3f
            r4 = r0
            goto L40
        L35:
            java.lang.String r6 = "ks"
            boolean r4 = r4.equals(r6)
            if (r4 == 0) goto L3f
            r4 = 2
            goto L40
        L3f:
            r4 = r5
        L40:
            switch(r4) {
                case 0: goto L51;
                case 1: goto L4c;
                case 2: goto L47;
                default: goto L43;
            }
        L43:
            r8.skipValue()
            goto L4
        L47:
            com.airbnb.lottie.model.animatable.AnimatableShapeValue r3 = com.airbnb.lottie.parser.AnimatableValueParser.parseShapeData(r8, r9)
            goto L4
        L4c:
            int r2 = r8.nextInt()
            goto L4
        L51:
            java.lang.String r1 = r8.nextString()
            goto L4
        L56:
            com.airbnb.lottie.model.content.ShapePath r8 = new com.airbnb.lottie.model.content.ShapePath
            r8.<init>(r1, r2, r3)
            return r8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.airbnb.lottie.parser.ShapePathParser.parse(android.util.JsonReader, com.airbnb.lottie.LottieComposition):com.airbnb.lottie.model.content.ShapePath");
    }
}
