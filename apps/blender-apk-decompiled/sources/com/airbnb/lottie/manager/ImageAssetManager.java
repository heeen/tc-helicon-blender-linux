package com.airbnb.lottie.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.airbnb.lottie.ImageAssetDelegate;
import com.airbnb.lottie.L;
import com.airbnb.lottie.LottieImageAsset;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class ImageAssetManager {
    private final Map<String, Bitmap> bitmaps = new HashMap();
    private final Context context;

    @Nullable
    private ImageAssetDelegate delegate;
    private final Map<String, LottieImageAsset> imageAssets;
    private String imagesFolder;

    public ImageAssetManager(Drawable.Callback callback, String str, ImageAssetDelegate imageAssetDelegate, Map<String, LottieImageAsset> map) {
        this.imagesFolder = str;
        if (!TextUtils.isEmpty(str) && this.imagesFolder.charAt(this.imagesFolder.length() - 1) != '/') {
            this.imagesFolder += '/';
        }
        if (!(callback instanceof View)) {
            Log.w(L.TAG, "LottieDrawable must be inside of a view for images to work.");
            this.imageAssets = new HashMap();
            this.context = null;
        } else {
            this.context = ((View) callback).getContext();
            this.imageAssets = map;
            setDelegate(imageAssetDelegate);
        }
    }

    public void setDelegate(@Nullable ImageAssetDelegate imageAssetDelegate) {
        this.delegate = imageAssetDelegate;
    }

    @Nullable
    public Bitmap updateBitmap(String str, @Nullable Bitmap bitmap) {
        if (bitmap == null) {
            return this.bitmaps.remove(str);
        }
        return this.bitmaps.put(str, bitmap);
    }

    @Nullable
    public Bitmap bitmapForId(String str) {
        Bitmap bitmap = this.bitmaps.get(str);
        if (bitmap != null) {
            return bitmap;
        }
        LottieImageAsset lottieImageAsset = this.imageAssets.get(str);
        if (lottieImageAsset == null) {
            return null;
        }
        if (this.delegate != null) {
            Bitmap bitmapFetchBitmap = this.delegate.fetchBitmap(lottieImageAsset);
            if (bitmapFetchBitmap != null) {
                this.bitmaps.put(str, bitmapFetchBitmap);
            }
            return bitmapFetchBitmap;
        }
        try {
            if (TextUtils.isEmpty(this.imagesFolder)) {
                throw new IllegalStateException("You must set an images folder before loading an image. Set it with LottieComposition#setImagesFolder or LottieDrawable#setImagesFolder");
            }
            InputStream inputStreamOpen = this.context.getAssets().open(this.imagesFolder + lottieImageAsset.getFileName());
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = true;
            options.inDensity = 160;
            Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpen, null, options);
            this.bitmaps.put(str, bitmapDecodeStream);
            return bitmapDecodeStream;
        } catch (IOException e) {
            Log.w(L.TAG, "Unable to open asset.", e);
            return null;
        }
    }

    public void recycleBitmaps() {
        Iterator<Map.Entry<String, Bitmap>> it = this.bitmaps.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().recycle();
            it.remove();
        }
    }

    public boolean hasSameContext(Context context) {
        return (context == null && this.context == null) || (context != null && this.context.equals(context));
    }
}
