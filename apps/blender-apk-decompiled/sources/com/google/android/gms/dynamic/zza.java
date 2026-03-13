package com.google.android.gms.dynamic;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.zzu;
import com.google.android.gms.dynamic.LifecycleDelegate;
import java.util.LinkedList;

/* JADX INFO: loaded from: classes.dex */
@Hide
public abstract class zza<T extends LifecycleDelegate> {
    private T zzhcp;
    private Bundle zzhcq;
    private LinkedList<zzi> zzhcr;
    private final zzo<T> zzhcs = new zzb(this);

    static /* synthetic */ Bundle zza(zza zzaVar, Bundle bundle) {
        zzaVar.zzhcq = null;
        return null;
    }

    private final void zza(Bundle bundle, zzi zziVar) {
        if (this.zzhcp != null) {
            zziVar.zzb(this.zzhcp);
            return;
        }
        if (this.zzhcr == null) {
            this.zzhcr = new LinkedList<>();
        }
        this.zzhcr.add(zziVar);
        if (bundle != null) {
            if (this.zzhcq == null) {
                this.zzhcq = (Bundle) bundle.clone();
            } else {
                this.zzhcq.putAll(bundle);
            }
        }
        zza(this.zzhcs);
    }

    static /* synthetic */ LifecycleDelegate zzb(zza zzaVar) {
        return zzaVar.zzhcp;
    }

    public static void zzb(FrameLayout frameLayout) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        Context context = frameLayout.getContext();
        int iIsGooglePlayServicesAvailable = googleApiAvailability.isGooglePlayServicesAvailable(context);
        String strZzh = zzu.zzh(context, iIsGooglePlayServicesAvailable);
        String strZzj = zzu.zzj(context, iIsGooglePlayServicesAvailable);
        LinearLayout linearLayout = new LinearLayout(frameLayout.getContext());
        linearLayout.setOrientation(1);
        linearLayout.setLayoutParams(new FrameLayout.LayoutParams(-2, -2));
        frameLayout.addView(linearLayout);
        TextView textView = new TextView(frameLayout.getContext());
        textView.setLayoutParams(new FrameLayout.LayoutParams(-2, -2));
        textView.setText(strZzh);
        linearLayout.addView(textView);
        Intent intentZza = com.google.android.gms.common.zzf.zza(context, iIsGooglePlayServicesAvailable, null);
        if (intentZza != null) {
            Button button = new Button(context);
            button.setId(R.id.button1);
            button.setLayoutParams(new FrameLayout.LayoutParams(-2, -2));
            button.setText(strZzj);
            linearLayout.addView(button);
            button.setOnClickListener(new zzf(context, intentZza));
        }
    }

    private final void zzcz(int i) {
        while (!this.zzhcr.isEmpty() && this.zzhcr.getLast().getState() >= i) {
            this.zzhcr.removeLast();
        }
    }

    public final void onCreate(Bundle bundle) {
        zza(bundle, new zzd(this, bundle));
    }

    public final View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        FrameLayout frameLayout = new FrameLayout(layoutInflater.getContext());
        zza(bundle, new zze(this, frameLayout, layoutInflater, viewGroup, bundle));
        if (this.zzhcp == null) {
            zza(frameLayout);
        }
        return frameLayout;
    }

    public final void onDestroy() {
        if (this.zzhcp != null) {
            this.zzhcp.onDestroy();
        } else {
            zzcz(1);
        }
    }

    public final void onDestroyView() {
        if (this.zzhcp != null) {
            this.zzhcp.onDestroyView();
        } else {
            zzcz(2);
        }
    }

    public final void onInflate(Activity activity, Bundle bundle, Bundle bundle2) {
        zza(bundle2, new zzc(this, activity, bundle, bundle2));
    }

    public final void onLowMemory() {
        if (this.zzhcp != null) {
            this.zzhcp.onLowMemory();
        }
    }

    public final void onPause() {
        if (this.zzhcp != null) {
            this.zzhcp.onPause();
        } else {
            zzcz(5);
        }
    }

    public final void onResume() {
        zza((Bundle) null, new zzh(this));
    }

    public final void onSaveInstanceState(Bundle bundle) {
        if (this.zzhcp != null) {
            this.zzhcp.onSaveInstanceState(bundle);
        } else if (this.zzhcq != null) {
            bundle.putAll(this.zzhcq);
        }
    }

    public final void onStart() {
        zza((Bundle) null, new zzg(this));
    }

    public final void onStop() {
        if (this.zzhcp != null) {
            this.zzhcp.onStop();
        } else {
            zzcz(4);
        }
    }

    protected void zza(FrameLayout frameLayout) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        Context context = frameLayout.getContext();
        int iIsGooglePlayServicesAvailable = googleApiAvailability.isGooglePlayServicesAvailable(context);
        String strZzh = zzu.zzh(context, iIsGooglePlayServicesAvailable);
        String strZzj = zzu.zzj(context, iIsGooglePlayServicesAvailable);
        LinearLayout linearLayout = new LinearLayout(frameLayout.getContext());
        linearLayout.setOrientation(1);
        linearLayout.setLayoutParams(new FrameLayout.LayoutParams(-2, -2));
        frameLayout.addView(linearLayout);
        TextView textView = new TextView(frameLayout.getContext());
        textView.setLayoutParams(new FrameLayout.LayoutParams(-2, -2));
        textView.setText(strZzh);
        linearLayout.addView(textView);
        Intent intentZza = com.google.android.gms.common.zzf.zza(context, iIsGooglePlayServicesAvailable, null);
        if (intentZza != null) {
            Button button = new Button(context);
            button.setId(R.id.button1);
            button.setLayoutParams(new FrameLayout.LayoutParams(-2, -2));
            button.setText(strZzj);
            linearLayout.addView(button);
            button.setOnClickListener(new zzf(context, intentZza));
        }
    }

    protected abstract void zza(zzo<T> zzoVar);

    public final T zzarg() {
        return this.zzhcp;
    }
}
