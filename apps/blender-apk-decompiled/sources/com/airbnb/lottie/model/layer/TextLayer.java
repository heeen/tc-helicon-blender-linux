package com.airbnb.lottie.model.layer;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.TextDelegate;
import com.airbnb.lottie.animation.content.ContentGroup;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.TextKeyframeAnimation;
import com.airbnb.lottie.model.DocumentData;
import com.airbnb.lottie.model.Font;
import com.airbnb.lottie.model.FontCharacter;
import com.airbnb.lottie.model.animatable.AnimatableTextProperties;
import com.airbnb.lottie.model.content.ShapeGroup;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.LottieValueCallback;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class TextLayer extends BaseLayer {

    @Nullable
    private BaseKeyframeAnimation<Integer, Integer> colorAnimation;
    private final LottieComposition composition;
    private final Map<FontCharacter, List<ContentGroup>> contentsForCharacter;
    private final Paint fillPaint;
    private final LottieDrawable lottieDrawable;
    private final Matrix matrix;
    private final RectF rectF;

    @Nullable
    private BaseKeyframeAnimation<Integer, Integer> strokeColorAnimation;
    private final Paint strokePaint;

    @Nullable
    private BaseKeyframeAnimation<Float, Float> strokeWidthAnimation;
    private final char[] tempCharArray;
    private final TextKeyframeAnimation textAnimation;

    @Nullable
    private BaseKeyframeAnimation<Float, Float> trackingAnimation;

    TextLayer(LottieDrawable lottieDrawable, Layer layer) {
        super(lottieDrawable, layer);
        int i = 1;
        this.tempCharArray = new char[1];
        this.rectF = new RectF();
        this.matrix = new Matrix();
        this.fillPaint = new Paint(i) { // from class: com.airbnb.lottie.model.layer.TextLayer.1
            {
                setStyle(Paint.Style.FILL);
            }
        };
        this.strokePaint = new Paint(i) { // from class: com.airbnb.lottie.model.layer.TextLayer.2
            {
                setStyle(Paint.Style.STROKE);
            }
        };
        this.contentsForCharacter = new HashMap();
        this.lottieDrawable = lottieDrawable;
        this.composition = layer.getComposition();
        this.textAnimation = layer.getText().createAnimation();
        this.textAnimation.addUpdateListener(this);
        addAnimation(this.textAnimation);
        AnimatableTextProperties textProperties = layer.getTextProperties();
        if (textProperties != null && textProperties.color != null) {
            this.colorAnimation = textProperties.color.createAnimation();
            this.colorAnimation.addUpdateListener(this);
            addAnimation(this.colorAnimation);
        }
        if (textProperties != null && textProperties.stroke != null) {
            this.strokeColorAnimation = textProperties.stroke.createAnimation();
            this.strokeColorAnimation.addUpdateListener(this);
            addAnimation(this.strokeColorAnimation);
        }
        if (textProperties != null && textProperties.strokeWidth != null) {
            this.strokeWidthAnimation = textProperties.strokeWidth.createAnimation();
            this.strokeWidthAnimation.addUpdateListener(this);
            addAnimation(this.strokeWidthAnimation);
        }
        if (textProperties == null || textProperties.tracking == null) {
            return;
        }
        this.trackingAnimation = textProperties.tracking.createAnimation();
        this.trackingAnimation.addUpdateListener(this);
        addAnimation(this.trackingAnimation);
    }

    @Override // com.airbnb.lottie.model.layer.BaseLayer
    void drawLayer(Canvas canvas, Matrix matrix, int i) {
        canvas.save();
        if (!this.lottieDrawable.useTextGlyphs()) {
            canvas.setMatrix(matrix);
        }
        DocumentData value = this.textAnimation.getValue();
        Font font = this.composition.getFonts().get(value.fontName);
        if (font == null) {
            canvas.restore();
            return;
        }
        if (this.colorAnimation != null) {
            this.fillPaint.setColor(this.colorAnimation.getValue().intValue());
        } else {
            this.fillPaint.setColor(value.color);
        }
        if (this.strokeColorAnimation != null) {
            this.strokePaint.setColor(this.strokeColorAnimation.getValue().intValue());
        } else {
            this.strokePaint.setColor(value.strokeColor);
        }
        int iIntValue = (this.transform.getOpacity().getValue().intValue() * 255) / 100;
        this.fillPaint.setAlpha(iIntValue);
        this.strokePaint.setAlpha(iIntValue);
        if (this.strokeWidthAnimation != null) {
            this.strokePaint.setStrokeWidth(this.strokeWidthAnimation.getValue().floatValue());
        } else {
            this.strokePaint.setStrokeWidth(value.strokeWidth * Utils.dpScale() * Utils.getScale(matrix));
        }
        if (this.lottieDrawable.useTextGlyphs()) {
            drawTextGlyphs(value, matrix, font, canvas);
        } else {
            drawTextWithFont(value, font, matrix, canvas);
        }
        canvas.restore();
    }

    private void drawTextGlyphs(DocumentData documentData, Matrix matrix, Font font, Canvas canvas) {
        float f = ((float) documentData.size) / 100.0f;
        float scale = Utils.getScale(matrix);
        String str = documentData.text;
        for (int i = 0; i < str.length(); i++) {
            FontCharacter fontCharacter = this.composition.getCharacters().get(FontCharacter.hashFor(str.charAt(i), font.getFamily(), font.getStyle()));
            if (fontCharacter != null) {
                drawCharacterAsGlyph(fontCharacter, matrix, f, documentData, canvas);
                float width = ((float) fontCharacter.getWidth()) * f * Utils.dpScale() * scale;
                float fFloatValue = documentData.tracking / 10.0f;
                if (this.trackingAnimation != null) {
                    fFloatValue += this.trackingAnimation.getValue().floatValue();
                }
                canvas.translate(width + (fFloatValue * scale), 0.0f);
            }
        }
    }

    private void drawTextWithFont(DocumentData documentData, Font font, Matrix matrix, Canvas canvas) {
        float scale = Utils.getScale(matrix);
        Typeface typeface = this.lottieDrawable.getTypeface(font.getFamily(), font.getStyle());
        if (typeface == null) {
            return;
        }
        String textInternal = documentData.text;
        TextDelegate textDelegate = this.lottieDrawable.getTextDelegate();
        if (textDelegate != null) {
            textInternal = textDelegate.getTextInternal(textInternal);
        }
        this.fillPaint.setTypeface(typeface);
        this.fillPaint.setTextSize((float) (documentData.size * ((double) Utils.dpScale())));
        this.strokePaint.setTypeface(this.fillPaint.getTypeface());
        this.strokePaint.setTextSize(this.fillPaint.getTextSize());
        for (int i = 0; i < textInternal.length(); i++) {
            char cCharAt = textInternal.charAt(i);
            drawCharacterFromFont(cCharAt, documentData, canvas);
            this.tempCharArray[0] = cCharAt;
            float fMeasureText = this.fillPaint.measureText(this.tempCharArray, 0, 1);
            float fFloatValue = documentData.tracking / 10.0f;
            if (this.trackingAnimation != null) {
                fFloatValue += this.trackingAnimation.getValue().floatValue();
            }
            canvas.translate(fMeasureText + (fFloatValue * scale), 0.0f);
        }
    }

    private void drawCharacterAsGlyph(FontCharacter fontCharacter, Matrix matrix, float f, DocumentData documentData, Canvas canvas) {
        List<ContentGroup> contentsForCharacter = getContentsForCharacter(fontCharacter);
        for (int i = 0; i < contentsForCharacter.size(); i++) {
            Path path = contentsForCharacter.get(i).getPath();
            path.computeBounds(this.rectF, false);
            this.matrix.set(matrix);
            this.matrix.preTranslate(0.0f, ((float) (-documentData.baselineShift)) * Utils.dpScale());
            this.matrix.preScale(f, f);
            path.transform(this.matrix);
            if (documentData.strokeOverFill) {
                drawGlyph(path, this.fillPaint, canvas);
                drawGlyph(path, this.strokePaint, canvas);
            } else {
                drawGlyph(path, this.strokePaint, canvas);
                drawGlyph(path, this.fillPaint, canvas);
            }
        }
    }

    private void drawGlyph(Path path, Paint paint, Canvas canvas) {
        if (paint.getColor() == 0) {
            return;
        }
        if (paint.getStyle() == Paint.Style.STROKE && paint.getStrokeWidth() == 0.0f) {
            return;
        }
        canvas.drawPath(path, paint);
    }

    private void drawCharacterFromFont(char c, DocumentData documentData, Canvas canvas) {
        this.tempCharArray[0] = c;
        if (documentData.strokeOverFill) {
            drawCharacter(this.tempCharArray, this.fillPaint, canvas);
            drawCharacter(this.tempCharArray, this.strokePaint, canvas);
        } else {
            drawCharacter(this.tempCharArray, this.strokePaint, canvas);
            drawCharacter(this.tempCharArray, this.fillPaint, canvas);
        }
    }

    private void drawCharacter(char[] cArr, Paint paint, Canvas canvas) {
        if (paint.getColor() == 0) {
            return;
        }
        if (paint.getStyle() == Paint.Style.STROKE && paint.getStrokeWidth() == 0.0f) {
            return;
        }
        canvas.drawText(cArr, 0, 1, 0.0f, 0.0f, paint);
    }

    private List<ContentGroup> getContentsForCharacter(FontCharacter fontCharacter) {
        if (this.contentsForCharacter.containsKey(fontCharacter)) {
            return this.contentsForCharacter.get(fontCharacter);
        }
        List<ShapeGroup> shapes = fontCharacter.getShapes();
        int size = shapes.size();
        ArrayList arrayList = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            arrayList.add(new ContentGroup(this.lottieDrawable, this, shapes.get(i)));
        }
        this.contentsForCharacter.put(fontCharacter, arrayList);
        return arrayList;
    }

    @Override // com.airbnb.lottie.model.layer.BaseLayer, com.airbnb.lottie.model.KeyPathElement
    public <T> void addValueCallback(T t, @Nullable LottieValueCallback<T> lottieValueCallback) {
        super.addValueCallback(t, lottieValueCallback);
        if (t == LottieProperty.COLOR && this.colorAnimation != null) {
            this.colorAnimation.setValueCallback(lottieValueCallback);
            return;
        }
        if (t == LottieProperty.STROKE_COLOR && this.strokeColorAnimation != null) {
            this.strokeColorAnimation.setValueCallback(lottieValueCallback);
            return;
        }
        if (t == LottieProperty.STROKE_WIDTH && this.strokeWidthAnimation != null) {
            this.strokeWidthAnimation.setValueCallback(lottieValueCallback);
        } else {
            if (t != LottieProperty.TEXT_TRACKING || this.trackingAnimation == null) {
                return;
            }
            this.trackingAnimation.setValueCallback(lottieValueCallback);
        }
    }
}
