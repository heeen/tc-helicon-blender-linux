package android.support.design.widget;

import android.graphics.Outline;
import android.support.annotation.RequiresApi;

/* JADX INFO: loaded from: classes.dex */
@RequiresApi(21)
class CircularBorderDrawableLollipop extends CircularBorderDrawable {
    CircularBorderDrawableLollipop() {
    }

    @Override // android.graphics.drawable.Drawable
    public void getOutline(Outline outline) {
        copyBounds(this.mRect);
        outline.setOval(this.mRect);
    }
}
