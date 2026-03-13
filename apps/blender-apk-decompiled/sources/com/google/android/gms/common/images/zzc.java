package com.google.android.gms.common.images;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.widget.ImageView;
import com.google.android.gms.common.internal.zzbg;
import com.google.android.gms.internal.zzbge;
import com.google.android.gms.internal.zzbgj;
import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes.dex */
public final class zzc extends zza {
    private WeakReference<ImageView> zzgdw;

    public zzc(ImageView imageView, int i) {
        super(null, i);
        com.google.android.gms.common.internal.zzc.zzv(imageView);
        this.zzgdw = new WeakReference<>(imageView);
    }

    public zzc(ImageView imageView, Uri uri) {
        super(uri, 0);
        com.google.android.gms.common.internal.zzc.zzv(imageView);
        this.zzgdw = new WeakReference<>(imageView);
    }

    public final boolean equals(Object obj) {
        if (!(obj instanceof zzc)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ImageView imageView = this.zzgdw.get();
        ImageView imageView2 = ((zzc) obj).zzgdw.get();
        return (imageView2 == null || imageView == null || !zzbg.equal(imageView2, imageView)) ? false : true;
    }

    public final int hashCode() {
        return 0;
    }

    @Override // com.google.android.gms.common.images.zza
    protected final void zza(Drawable drawable, boolean z, boolean z2, boolean z3) {
        ImageView imageView = this.zzgdw.get();
        if (imageView != null) {
            boolean z4 = (z2 || z3) ? false : true;
            if (z4 && (imageView instanceof zzbgj)) {
                int iZzalp = zzbgj.zzalp();
                if (this.zzgdr != 0 && iZzalp == this.zzgdr) {
                    return;
                }
            }
            boolean zZzc = zzc(z, z2);
            if (zZzc) {
                Drawable drawable2 = imageView.getDrawable();
                if (drawable2 == null) {
                    drawable2 = null;
                } else if (drawable2 instanceof zzbge) {
                    drawable2 = ((zzbge) drawable2).zzaln();
                }
                drawable = new zzbge(drawable2, drawable);
            }
            imageView.setImageDrawable(drawable);
            if (imageView instanceof zzbgj) {
                zzbgj.zzn(z3 ? this.zzgdp.uri : null);
                zzbgj.zzcc(z4 ? this.zzgdr : 0);
            }
            if (zZzc) {
                ((zzbge) drawable).startTransition(ItemTouchHelper.Callback.DEFAULT_SWIPE_ANIMATION_DURATION);
            }
        }
    }
}
