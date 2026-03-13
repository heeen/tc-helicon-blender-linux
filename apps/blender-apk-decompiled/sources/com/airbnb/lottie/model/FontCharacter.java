package com.airbnb.lottie.model;

import com.airbnb.lottie.model.content.ShapeGroup;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class FontCharacter {
    private final char character;
    private final String fontFamily;
    private final List<ShapeGroup> shapes;
    private final int size;
    private final String style;
    private final double width;

    public static int hashFor(char c, String str, String str2) {
        return ((((0 + c) * 31) + str.hashCode()) * 31) + str2.hashCode();
    }

    public FontCharacter(List<ShapeGroup> list, char c, int i, double d, String str, String str2) {
        this.shapes = list;
        this.character = c;
        this.size = i;
        this.width = d;
        this.style = str;
        this.fontFamily = str2;
    }

    public List<ShapeGroup> getShapes() {
        return this.shapes;
    }

    int getSize() {
        return this.size;
    }

    public double getWidth() {
        return this.width;
    }

    String getStyle() {
        return this.style;
    }

    public int hashCode() {
        return hashFor(this.character, this.fontFamily, this.style);
    }
}
