package com.google.android.gms.common.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.internal.zzbgk;

/* JADX INFO: loaded from: classes.dex */
@Hide
public abstract class zza {
    final zzb zzgdp;
    protected int zzgdr;
    private int zzgdq = 0;
    private boolean zzgds = false;
    private boolean zzgdt = true;
    private boolean zzgdu = false;
    private boolean zzgdv = true;

    public zza(Uri uri, int i) {
        this.zzgdr = 0;
        this.zzgdp = new zzb(uri);
        this.zzgdr = i;
    }

    final void zza(Context context, Bitmap bitmap, boolean z) {
        com.google.android.gms.common.internal.zzc.zzv(bitmap);
        zza(new BitmapDrawable(context.getResources(), bitmap), z, false, true);
    }

    final void zza(Context context, zzbgk zzbgkVar) {
        if (this.zzgdv) {
            zza(null, false, true, false);
        }
    }

    final void zza(Context context, zzbgk zzbgkVar, boolean z) {
        Drawable drawable;
        if (this.zzgdr != 0) {
            drawable = context.getResources().getDrawable(this.zzgdr);
        } else {
            drawable = null;
        }
        zza(drawable, z, false, false);
    }

    protected abstract void zza(Drawable drawable, boolean z, boolean z2, boolean z3);

    protected final boolean zzc(boolean z, boolean z2) {
        return (!this.zzgdt || z2 || z) ? false : true;
    }
}
