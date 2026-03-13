package com.airbnb.lottie.parser;

import android.os.AsyncTask;
import android.util.JsonReader;
import com.airbnb.lottie.Cancellable;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.OnCompositionLoadedListener;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class AsyncCompositionLoader extends AsyncTask<JsonReader, Void, LottieComposition> implements Cancellable {
    private final OnCompositionLoadedListener loadedListener;

    public AsyncCompositionLoader(OnCompositionLoadedListener onCompositionLoadedListener) {
        this.loadedListener = onCompositionLoadedListener;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.os.AsyncTask
    public LottieComposition doInBackground(JsonReader... jsonReaderArr) {
        try {
            return LottieComposition.Factory.fromJsonSync(jsonReaderArr[0]);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.os.AsyncTask
    public void onPostExecute(LottieComposition lottieComposition) {
        this.loadedListener.onCompositionLoaded(lottieComposition);
    }

    @Override // com.airbnb.lottie.Cancellable
    public void cancel() {
        cancel(true);
    }
}
