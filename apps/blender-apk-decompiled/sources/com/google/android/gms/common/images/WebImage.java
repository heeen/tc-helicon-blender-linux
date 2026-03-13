package com.google.android.gms.common.images;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzbg;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.util.Arrays;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public final class WebImage extends zzbgl {
    public static final Parcelable.Creator<WebImage> CREATOR = new zze();
    private final int zzalt;
    private final int zzalu;
    private int zzehz;
    private final Uri zzfgr;

    WebImage(int i, Uri uri, int i2, int i3) {
        this.zzehz = i;
        this.zzfgr = uri;
        this.zzalt = i2;
        this.zzalu = i3;
    }

    public WebImage(Uri uri) throws IllegalArgumentException {
        this(uri, 0, 0);
    }

    public WebImage(Uri uri, int i, int i2) throws IllegalArgumentException {
        this(1, uri, i, i2);
        if (uri == null) {
            throw new IllegalArgumentException("url cannot be null");
        }
        if (i < 0 || i2 < 0) {
            throw new IllegalArgumentException("width and height must not be negative");
        }
    }

    @Hide
    public WebImage(JSONObject jSONObject) throws IllegalArgumentException {
        this(zzx(jSONObject), jSONObject.optInt("width", 0), jSONObject.optInt("height", 0));
    }

    private static Uri zzx(JSONObject jSONObject) {
        if (jSONObject.has("url")) {
            try {
                return Uri.parse(jSONObject.getString("url"));
            } catch (JSONException unused) {
            }
        }
        return null;
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && (obj instanceof WebImage)) {
            WebImage webImage = (WebImage) obj;
            if (zzbg.equal(this.zzfgr, webImage.zzfgr) && this.zzalt == webImage.zzalt && this.zzalu == webImage.zzalu) {
                return true;
            }
        }
        return false;
    }

    public final int getHeight() {
        return this.zzalu;
    }

    public final Uri getUrl() {
        return this.zzfgr;
    }

    public final int getWidth() {
        return this.zzalt;
    }

    public final int hashCode() {
        return Arrays.hashCode(new Object[]{this.zzfgr, Integer.valueOf(this.zzalt), Integer.valueOf(this.zzalu)});
    }

    @Hide
    public final JSONObject toJson() {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("url", this.zzfgr.toString());
            jSONObject.put("width", this.zzalt);
            jSONObject.put("height", this.zzalu);
        } catch (JSONException unused) {
        }
        return jSONObject;
    }

    public final String toString() {
        return String.format(Locale.US, "Image %dx%d %s", Integer.valueOf(this.zzalt), Integer.valueOf(this.zzalu), this.zzfgr.toString());
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.zzehz);
        zzbgo.zza(parcel, 2, (Parcelable) getUrl(), i, false);
        zzbgo.zzc(parcel, 3, getWidth());
        zzbgo.zzc(parcel, 4, getHeight());
        zzbgo.zzai(parcel, iZze);
    }
}
