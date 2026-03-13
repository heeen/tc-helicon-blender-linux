package android.support.v4.os;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.media.MediaDescriptionCompat;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
interface LocaleListInterface {
    boolean equals(Object obj);

    Locale get(int i);

    @Nullable
    Locale getFirstMatch(String[] strArr);

    Object getLocaleList();

    int hashCode();

    @IntRange(from = -1)
    int indexOf(Locale locale);

    boolean isEmpty();

    void setLocaleList(@NonNull Locale... localeArr);

    @IntRange(from = MediaDescriptionCompat.BT_FOLDER_TYPE_MIXED)
    int size();

    String toLanguageTags();

    String toString();
}
