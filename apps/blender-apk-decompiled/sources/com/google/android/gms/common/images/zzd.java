package com.google.android.gms.common.images;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.common.internal.zzbg;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class zzd extends zza {
    private WeakReference<ImageManager.OnImageLoadedListener> zzgdx;

    public zzd(ImageManager.OnImageLoadedListener onImageLoadedListener, Uri uri) {
        super(uri, 0);
        com.google.android.gms.common.internal.zzc.zzv(onImageLoadedListener);
        this.zzgdx = new WeakReference<>(onImageLoadedListener);
    }

    public final boolean equals(Object obj) {
        if (!(obj instanceof zzd)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        zzd zzdVar = (zzd) obj;
        ImageManager.OnImageLoadedListener onImageLoadedListener = this.zzgdx.get();
        ImageManager.OnImageLoadedListener onImageLoadedListener2 = zzdVar.zzgdx.get();
        return onImageLoadedListener2 != null && onImageLoadedListener != null && zzbg.equal(onImageLoadedListener2, onImageLoadedListener) && zzbg.equal(zzdVar.zzgdp, this.zzgdp);
    }

    public final int hashCode() {
        return Arrays.hashCode(new Object[]{this.zzgdp});
    }

    @Override // com.google.android.gms.common.images.zza
    protected final void zza(Drawable drawable, boolean z, boolean z2, boolean z3) {
        ImageManager.OnImageLoadedListener onImageLoadedListener;
        if (z2 || (onImageLoadedListener = this.zzgdx.get()) == null) {
            return;
        }
        onImageLoadedListener.onImageLoaded(this.zzgdp.uri, drawable, z3);
    }
}
