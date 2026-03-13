package com.google.firebase.iid;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/* JADX INFO: loaded from: classes.dex */
final class zzaa {
    private Context zzaiq;
    private SharedPreferences zzioc;

    public zzaa(Context context) {
        this(context, "com.google.android.gms.appid");
    }

    private zzaa(Context context, String str) {
        this.zzaiq = context;
        this.zzioc = context.getSharedPreferences(str, 0);
        String strValueOf = String.valueOf(str);
        String strValueOf2 = String.valueOf("-no-backup");
        File file = new File(com.google.android.gms.common.util.zzx.getNoBackupFilesDir(this.zzaiq), strValueOf2.length() != 0 ? strValueOf.concat(strValueOf2) : new String(strValueOf));
        if (file.exists()) {
            return;
        }
        try {
            if (!file.createNewFile() || isEmpty()) {
                return;
            }
            Log.i("FirebaseInstanceId", "App restored, clearing state");
            zzawz();
            FirebaseInstanceId.getInstance().zzclg();
        } catch (IOException e) {
            if (Log.isLoggable("FirebaseInstanceId", 3)) {
                String strValueOf3 = String.valueOf(e.getMessage());
                Log.d("FirebaseInstanceId", strValueOf3.length() != 0 ? "Error creating file in no backup dir: ".concat(strValueOf3) : new String("Error creating file in no backup dir: "));
            }
        }
    }

    private final synchronized boolean isEmpty() {
        return this.zzioc.getAll().isEmpty();
    }

    private static String zzbk(String str, String str2) {
        StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 3 + String.valueOf(str2).length());
        sb.append(str);
        sb.append("|S|");
        sb.append(str2);
        return sb.toString();
    }

    private static String zzp(String str, String str2, String str3) {
        StringBuilder sb = new StringBuilder(String.valueOf(str).length() + 4 + String.valueOf(str2).length() + String.valueOf(str3).length());
        sb.append(str);
        sb.append("|T|");
        sb.append(str2);
        sb.append("|");
        sb.append(str3);
        return sb.toString();
    }

    public final synchronized void zza(String str, String str2, String str3, String str4, String str5) {
        String strZzc = zzab.zzc(str4, str5, System.currentTimeMillis());
        if (strZzc == null) {
            return;
        }
        SharedPreferences.Editor editorEdit = this.zzioc.edit();
        editorEdit.putString(zzp(str, str2, str3), strZzc);
        editorEdit.commit();
    }

    public final synchronized void zzawz() {
        this.zzioc.edit().clear().commit();
    }

    @Nullable
    public final synchronized String zzcls() {
        String string = this.zzioc.getString("topic_operaion_queue", null);
        if (string != null) {
            String[] strArrSplit = string.split(",");
            if (strArrSplit.length > 1 && !TextUtils.isEmpty(strArrSplit[1])) {
                return strArrSplit[1];
            }
        }
        return null;
    }

    public final synchronized void zzg(String str, String str2, String str3) {
        String strZzp = zzp(str, str2, str3);
        SharedPreferences.Editor editorEdit = this.zzioc.edit();
        editorEdit.remove(strZzp);
        editorEdit.commit();
    }

    public final synchronized zzab zzq(String str, String str2, String str3) {
        return zzab.zzrt(this.zzioc.getString(zzp(str, str2, str3), null));
    }

    public final synchronized void zzrl(String str) {
        String string = this.zzioc.getString("topic_operaion_queue", "");
        StringBuilder sb = new StringBuilder(String.valueOf(string).length() + 1 + String.valueOf(str).length());
        sb.append(string);
        sb.append(",");
        sb.append(str);
        this.zzioc.edit().putString("topic_operaion_queue", sb.toString()).apply();
    }

    public final synchronized boolean zzro(String str) {
        boolean z;
        String string = this.zzioc.getString("topic_operaion_queue", "");
        String strValueOf = String.valueOf(",");
        String strValueOf2 = String.valueOf(str);
        if (string.startsWith(strValueOf2.length() != 0 ? strValueOf.concat(strValueOf2) : new String(strValueOf))) {
            String strValueOf3 = String.valueOf(",");
            String strValueOf4 = String.valueOf(str);
            this.zzioc.edit().putString("topic_operaion_queue", string.substring((strValueOf4.length() != 0 ? strValueOf3.concat(strValueOf4) : new String(strValueOf3)).length())).apply();
            z = true;
        } else {
            z = false;
        }
        return z;
    }

    public final synchronized long zzrp(String str) {
        String string = this.zzioc.getString(zzbk(str, "cre"), null);
        if (string != null) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException unused) {
            }
        }
        return 0L;
    }

    final synchronized KeyPair zzrq(String str) {
        KeyPair keyPairZzawn;
        keyPairZzawn = zza.zzawn();
        long jCurrentTimeMillis = System.currentTimeMillis();
        SharedPreferences.Editor editorEdit = this.zzioc.edit();
        editorEdit.putString(zzbk(str, "|P|"), Base64.encodeToString(keyPairZzawn.getPublic().getEncoded(), 11));
        editorEdit.putString(zzbk(str, "|K|"), Base64.encodeToString(keyPairZzawn.getPrivate().getEncoded(), 11));
        editorEdit.putString(zzbk(str, "cre"), Long.toString(jCurrentTimeMillis));
        editorEdit.commit();
        return keyPairZzawn;
    }

    public final synchronized void zzrr(String str) {
        String strConcat = String.valueOf(str).concat("|T|");
        SharedPreferences.Editor editorEdit = this.zzioc.edit();
        for (String str2 : this.zzioc.getAll().keySet()) {
            if (str2.startsWith(strConcat)) {
                editorEdit.remove(str2);
            }
        }
        editorEdit.commit();
    }

    public final synchronized KeyPair zzrs(String str) {
        String string = this.zzioc.getString(zzbk(str, "|P|"), null);
        String string2 = this.zzioc.getString(zzbk(str, "|K|"), null);
        if (string == null || string2 == null) {
            return null;
        }
        try {
            byte[] bArrDecode = Base64.decode(string, 8);
            byte[] bArrDecode2 = Base64.decode(string2, 8);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return new KeyPair(keyFactory.generatePublic(new X509EncodedKeySpec(bArrDecode)), keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bArrDecode2)));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            String strValueOf = String.valueOf(e);
            StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 19);
            sb.append("Invalid key stored ");
            sb.append(strValueOf);
            Log.w("FirebaseInstanceId", sb.toString());
            FirebaseInstanceId.getInstance().zzclg();
            return null;
        }
    }
}
