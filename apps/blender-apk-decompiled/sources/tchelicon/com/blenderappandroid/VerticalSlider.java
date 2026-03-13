package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.widget.SeekBar;

/* JADX INFO: loaded from: classes.dex */
public class VerticalSlider extends AppCompatSeekBar {
    private static final String TAG = "MixController";
    private SeekBar.OnSeekBarChangeListener onChangeListener;

    public VerticalSlider(Context context) {
        super(context);
    }

    public VerticalSlider(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public VerticalSlider(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.widget.SeekBar
    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener onSeekBarChangeListener) {
        this.onChangeListener = onSeekBarChangeListener;
    }

    @Override // android.widget.AbsSeekBar, android.widget.ProgressBar, android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i2, i, i4, i3);
    }

    @Override // android.widget.AbsSeekBar, android.widget.ProgressBar, android.view.View
    protected synchronized void onMeasure(int i, int i2) {
        super.onMeasure(i2, i);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override // android.support.v7.widget.AppCompatSeekBar, android.widget.AbsSeekBar, android.widget.ProgressBar, android.view.View
    protected void onDraw(Canvas canvas) {
        canvas.rotate(-90.0f);
        canvas.translate(-getHeight(), 0.0f);
        super.onDraw(canvas);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0036  */
    /* JADX WARN: Removed duplicated region for block: B:19:0x0063  */
    @Override // android.widget.AbsSeekBar, android.view.View
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean onTouchEvent(android.view.MotionEvent r5) {
        /*
            r4 = this;
            boolean r0 = r4.isEnabled()
            r1 = 0
            if (r0 != 0) goto L8
            return r1
        L8:
            int r0 = r5.getAction()
            r2 = 1
            switch(r0) {
                case 0: goto L1b;
                case 1: goto L2b;
                case 2: goto L3b;
                case 3: goto L11;
                default: goto L10;
            }
        L10:
            goto L6c
        L11:
            android.widget.SeekBar$OnSeekBarChangeListener r5 = r4.onChangeListener
            if (r5 == 0) goto L6c
            android.widget.SeekBar$OnSeekBarChangeListener r5 = r4.onChangeListener
            r5.onStopTrackingTouch(r4)
            goto L6c
        L1b:
            java.lang.String r0 = "MixController"
            java.lang.String r3 = "ACTION_DOWN"
            android.util.Log.d(r0, r3)
            android.widget.SeekBar$OnSeekBarChangeListener r0 = r4.onChangeListener
            if (r0 == 0) goto L2b
            android.widget.SeekBar$OnSeekBarChangeListener r0 = r4.onChangeListener
            r0.onStartTrackingTouch(r4)
        L2b:
            java.lang.String r0 = "MixController"
            java.lang.String r3 = "ACTION_UP"
            android.util.Log.d(r0, r3)
            android.widget.SeekBar$OnSeekBarChangeListener r0 = r4.onChangeListener
            if (r0 == 0) goto L3b
            android.widget.SeekBar$OnSeekBarChangeListener r0 = r4.onChangeListener
            r0.onStopTrackingTouch(r4)
        L3b:
            int r0 = r4.getMax()
            int r3 = r4.getMax()
            float r3 = (float) r3
            float r5 = r5.getY()
            float r3 = r3 * r5
            int r5 = r4.getHeight()
            float r5 = (float) r5
            float r3 = r3 / r5
            int r5 = (int) r3
            int r0 = r0 - r5
            r4.setProgress(r0)
            int r5 = r4.getWidth()
            int r0 = r4.getHeight()
            r4.onSizeChanged(r5, r0, r1, r1)
            android.widget.SeekBar$OnSeekBarChangeListener r5 = r4.onChangeListener
            if (r5 == 0) goto L6c
            android.widget.SeekBar$OnSeekBarChangeListener r5 = r4.onChangeListener
            int r0 = r4.getProgress()
            r5.onProgressChanged(r4, r0, r2)
        L6c:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: tchelicon.com.blenderappandroid.VerticalSlider.onTouchEvent(android.view.MotionEvent):boolean");
    }
}
