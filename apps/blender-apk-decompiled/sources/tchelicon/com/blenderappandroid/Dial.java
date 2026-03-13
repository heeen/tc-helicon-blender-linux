package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.ArcShape;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/* JADX INFO: loaded from: classes.dex */
public class Dial extends ConstraintLayout {
    private static final String TAG = "Dial";
    private DialListener dialListener;
    DialModel model;
    private float startX;
    private float startY;

    public interface DialListener {
        void onEndEditing(DialModel dialModel);

        void onStartEditing(DialModel dialModel);

        void valueChanged(DialModel dialModel);
    }

    private void setup() {
    }

    public void setDialListener(DialListener dialListener) {
        this.dialListener = dialListener;
    }

    public Dial(Context context) {
        super(context);
        this.model = DialModel.defaultDialModels[0];
        Log.d(TAG, "context init");
        setup();
    }

    public Dial(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.model = DialModel.defaultDialModels[0];
        Log.d(TAG, "context/attrs init");
        setup();
    }

    public Dial(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.model = DialModel.defaultDialModels[0];
        Log.d(TAG, "context/attrs/defStyle init");
        setup();
    }

    public void onCreate(DialModel dialModel) {
        Log.d(TAG, "OnCreate");
        this.model = dialModel;
        setTouchEvents();
        setup();
        updateTo(dialModel.value);
        setBackgroundResource(dialModel.background);
        ringMask().setImageTintList(ContextCompat.getColorStateList(getContext(), dialModel.darkTrack));
    }

    public void updateUI() {
        updateTo(this.model.value);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTo(Float f) {
        float fFloatValue = (f.floatValue() * (this.model.maxAngle - this.model.minAngle)) + this.model.minAngle;
        if (fFloatValue < this.model.minAngle || fFloatValue > this.model.maxAngle) {
            fFloatValue = this.model.maxAngle;
        }
        ImageView thumb = getThumb();
        if (thumb != null) {
            thumb.setRotation(fFloatValue);
        }
        calculateMask();
    }

    void calculateMask() {
        ImageView imageViewRing = ring();
        if (imageViewRing != null) {
            new ArcShape(this.model.minAngle, (this.model.value.floatValue() * (this.model.maxAngle - this.model.minAngle)) + this.model.minAngle).resize(getWidth(), getHeight());
            imageViewRing.setImageDrawable(new Drawable() { // from class: tchelicon.com.blenderappandroid.Dial.1
                @Override // android.graphics.drawable.Drawable
                public int getOpacity() {
                    return 0;
                }

                @Override // android.graphics.drawable.Drawable
                public void setAlpha(int i) {
                }

                @Override // android.graphics.drawable.Drawable
                public void setColorFilter(@Nullable ColorFilter colorFilter) {
                }

                @Override // android.graphics.drawable.Drawable
                public void draw(@NonNull Canvas canvas) {
                    Bitmap bitmapDecodeResource = BitmapFactory.decodeResource(Dial.this.getResources(), Dial.this.model.track);
                    int width = bitmapDecodeResource.getWidth();
                    int height = bitmapDecodeResource.getHeight();
                    RectF rectF = new RectF(0.0f, 0.0f, width, height);
                    Path path = new Path();
                    float f = width / 2;
                    float f2 = height / 2;
                    path.lineTo(f, f2);
                    path.addArc(rectF, Dial.this.model.minAngle, Dial.this.model.value.floatValue() * (Dial.this.model.maxAngle - Dial.this.model.minAngle));
                    path.lineTo(f, f2);
                    Rect rect = new Rect(0, 0, width, height);
                    RectF rectF2 = new RectF(0.0f, 0.0f, Dial.this.getWidth(), Dial.this.getHeight());
                    Bitmap croppedBitmap = BitmapUtils.getCroppedBitmap(bitmapDecodeResource, path);
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setFilterBitmap(true);
                    canvas.drawBitmap(croppedBitmap, rect, rectF2, paint);
                }
            });
        }
    }

    private void setTouchEvents() {
        setOnTouchListener(new View.OnTouchListener() { // from class: tchelicon.com.blenderappandroid.Dial.2
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case 0:
                        Dial.this.startX = motionEvent.getX();
                        Dial.this.startY = motionEvent.getY();
                        if (Dial.this.dialListener != null) {
                            Dial.this.dialListener.onStartEditing(Dial.this.model);
                        }
                        break;
                    case 1:
                        if (Dial.this.dialListener != null) {
                            Dial.this.dialListener.onEndEditing(Dial.this.model);
                        }
                        break;
                    case 2:
                        float x = motionEvent.getX();
                        float y = motionEvent.getY();
                        float f = (x - Dial.this.startX) - (y - Dial.this.startY);
                        DialModel dialModel = Dial.this.model;
                        dialModel.value = Float.valueOf(dialModel.value.floatValue() + (f / (Dial.this.getWidth() * 0.85f)));
                        if (Dial.this.model.value.floatValue() > 1.0f) {
                            Dial.this.model.value = Float.valueOf(1.0f);
                        } else if (Dial.this.model.value.floatValue() < 0.0f) {
                            Dial.this.model.value = Float.valueOf(0.0f);
                        }
                        Dial.this.startX = x;
                        Dial.this.startY = y;
                        Dial.this.updateTo(Dial.this.model.value);
                        if (Dial.this.dialListener != null) {
                            Dial.this.dialListener.valueChanged(Dial.this.model);
                        }
                        break;
                }
                return true;
            }
        });
    }

    private ImageView getThumb() {
        return (ImageView) findViewById(R.id.thumb);
    }

    private ImageView ring() {
        return (ImageView) findViewById(R.id.ring);
    }

    private ImageView ringMask() {
        return (ImageView) findViewById(R.id.ringMask);
    }
}
