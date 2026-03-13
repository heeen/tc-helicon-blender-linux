package com.google.android.gms.common.api;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.internal.zzbm;
import com.google.android.gms.common.internal.Hide;

/* JADX INFO: loaded from: classes.dex */
public class GoogleApiActivity extends Activity implements DialogInterface.OnCancelListener {

    @Hide
    private int zzfsu = 0;

    @Hide
    public GoogleApiActivity() {
    }

    @Hide
    public static PendingIntent zza(Context context, PendingIntent pendingIntent, int i) {
        return PendingIntent.getActivity(context, 0, zza(context, pendingIntent, i, true), 134217728);
    }

    @Hide
    public static Intent zza(Context context, PendingIntent pendingIntent, int i, boolean z) {
        Intent intent = new Intent(context, (Class<?>) GoogleApiActivity.class);
        intent.putExtra("pending_intent", pendingIntent);
        intent.putExtra("failing_client_id", i);
        intent.putExtra("notify_manager", z);
        return intent;
    }

    @Override // android.app.Activity
    @Hide
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 1) {
            boolean booleanExtra = getIntent().getBooleanExtra("notify_manager", true);
            this.zzfsu = 0;
            setResult(i2, intent);
            if (booleanExtra) {
                zzbm zzbmVarZzck = zzbm.zzck(this);
                switch (i2) {
                    case -1:
                        zzbmVarZzck.zzaih();
                        break;
                    case 0:
                        zzbmVarZzck.zza(new ConnectionResult(13, null), getIntent().getIntExtra("failing_client_id", -1));
                        break;
                }
            }
        } else if (i == 2) {
            this.zzfsu = 0;
            setResult(i2, intent);
        }
        finish();
    }

    @Override // android.content.DialogInterface.OnCancelListener
    @Hide
    public void onCancel(DialogInterface dialogInterface) {
        this.zzfsu = 0;
        setResult(0);
        finish();
    }

    @Override // android.app.Activity
    @Hide
    protected void onCreate(Bundle bundle) {
        String str;
        String str2;
        super.onCreate(bundle);
        if (bundle != null) {
            this.zzfsu = bundle.getInt("resolution");
        }
        if (this.zzfsu != 1) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                str = "GoogleApiActivity";
                str2 = "Activity started without extras";
            } else {
                PendingIntent pendingIntent = (PendingIntent) extras.get("pending_intent");
                Integer num = (Integer) extras.get("error_code");
                if (pendingIntent == null && num == null) {
                    str = "GoogleApiActivity";
                    str2 = "Activity started without resolution";
                } else if (pendingIntent == null) {
                    GoogleApiAvailability.getInstance().showErrorDialogFragment(this, num.intValue(), 2, this);
                    this.zzfsu = 1;
                    return;
                } else {
                    try {
                        startIntentSenderForResult(pendingIntent.getIntentSender(), 1, null, 0, 0, 0);
                        this.zzfsu = 1;
                        return;
                    } catch (IntentSender.SendIntentException e) {
                        Log.e("GoogleApiActivity", "Failed to launch pendingIntent", e);
                    }
                }
            }
            Log.e(str, str2);
            finish();
        }
    }

    @Override // android.app.Activity
    @Hide
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putInt("resolution", this.zzfsu);
        super.onSaveInstanceState(bundle);
    }
}
