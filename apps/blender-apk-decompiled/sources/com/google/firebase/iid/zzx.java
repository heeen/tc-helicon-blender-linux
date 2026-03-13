package com.google.firebase.iid;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.iid.zzi;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class zzx {
    private static PendingIntent zzikb;
    private static int zzino;
    private final Context zzaiq;
    private Messenger zziny;
    private final zzw zzokq;
    private zzi zzolh;
    private final SimpleArrayMap<String, TaskCompletionSource<Bundle>> zzolg = new SimpleArrayMap<>();
    private Messenger zzikf = new Messenger(new zzy(this, Looper.getMainLooper()));

    public zzx(Context context, zzw zzwVar) {
        this.zzaiq = context;
        this.zzokq = zzwVar;
    }

    /* JADX WARN: Can't wrap try/catch for region: R(11:8|(1:10)(1:12)|11|13|(1:15)|16|(5:(7:18|(0)(2:29|(1:31)(1:32))|66|33|34|eb|38)|66|33|34|eb)|20|67|21|(1:23)(1:24)) */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x00b9, code lost:
    
        r1 = r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x00bf, code lost:
    
        if (android.util.Log.isLoggable("FirebaseInstanceId", r3) != false) goto L28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x00c1, code lost:
    
        android.util.Log.d("FirebaseInstanceId", "Messenger failed, fallback to startService");
        r1 = r1;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:31:0x00d0  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x00d6  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x00ec A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r1v0, types: [com.google.android.gms.tasks.TaskCompletionSource, java.lang.Object] */
    /* JADX WARN: Type inference failed for: r1v1 */
    /* JADX WARN: Type inference failed for: r1v10 */
    /* JADX WARN: Type inference failed for: r1v11 */
    /* JADX WARN: Type inference failed for: r1v12 */
    /* JADX WARN: Type inference failed for: r1v7 */
    /* JADX WARN: Type inference failed for: r1v9 */
    /* JADX WARN: Type inference failed for: r3v12 */
    /* JADX WARN: Type inference failed for: r3v15, types: [java.util.concurrent.TimeUnit] */
    /* JADX WARN: Type inference fix 'apply assigned field type' failed
    java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$UnknownArg
    	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:593)
    	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
    	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
     */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:31:0x00d0 -> B:66:0x00db). Please report as a decompilation issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:32:0x00d6 -> B:66:0x00db). Please report as a decompilation issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private final android.os.Bundle zzaa(android.os.Bundle r9) throws java.io.IOException {
        /*
            Method dump skipped, instruction units count: 294
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.firebase.iid.zzx.zzaa(android.os.Bundle):android.os.Bundle");
    }

    private static synchronized String zzawx() {
        int i;
        i = zzino;
        zzino = i + 1;
        return Integer.toString(i);
    }

    @Hide
    private static synchronized void zzd(Context context, Intent intent) {
        if (zzikb == null) {
            Intent intent2 = new Intent();
            intent2.setPackage("com.google.example.invalidpackage");
            zzikb = PendingIntent.getBroadcast(context, 0, intent2, 0);
        }
        intent.putExtra("app", zzikb);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void zze(Message message) {
        if (message == null || !(message.obj instanceof Intent)) {
            Log.w("FirebaseInstanceId", "Dropping invalid message");
            return;
        }
        Intent intent = (Intent) message.obj;
        intent.setExtrasClassLoader(new zzi.zza());
        if (intent.hasExtra("google.messenger")) {
            Parcelable parcelableExtra = intent.getParcelableExtra("google.messenger");
            if (parcelableExtra instanceof zzi) {
                this.zzolh = (zzi) parcelableExtra;
            }
            if (parcelableExtra instanceof Messenger) {
                this.zziny = (Messenger) parcelableExtra;
            }
        }
        Intent intent2 = (Intent) message.obj;
        String action = intent2.getAction();
        if (!"com.google.android.c2dm.intent.REGISTRATION".equals(action)) {
            if (Log.isLoggable("FirebaseInstanceId", 3)) {
                String strValueOf = String.valueOf(action);
                Log.d("FirebaseInstanceId", strValueOf.length() != 0 ? "Unexpected response action: ".concat(strValueOf) : new String("Unexpected response action: "));
                return;
            }
            return;
        }
        String stringExtra = intent2.getStringExtra("registration_id");
        if (stringExtra == null) {
            stringExtra = intent2.getStringExtra("unregistered");
        }
        if (stringExtra == null) {
            zzr(intent2);
            return;
        }
        Matcher matcher = Pattern.compile("\\|ID\\|([^|]+)\\|:?+(.*)").matcher(stringExtra);
        if (!matcher.matches()) {
            if (Log.isLoggable("FirebaseInstanceId", 3)) {
                String strValueOf2 = String.valueOf(stringExtra);
                Log.d("FirebaseInstanceId", strValueOf2.length() != 0 ? "Unexpected response string: ".concat(strValueOf2) : new String("Unexpected response string: "));
                return;
            }
            return;
        }
        String strGroup = matcher.group(1);
        String strGroup2 = matcher.group(2);
        Bundle extras = intent2.getExtras();
        extras.putString("registration_id", strGroup2);
        zzh(strGroup, extras);
    }

    private final void zzh(String str, Bundle bundle) {
        synchronized (this.zzolg) {
            TaskCompletionSource<Bundle> taskCompletionSourceRemove = this.zzolg.remove(str);
            if (taskCompletionSourceRemove != null) {
                taskCompletionSourceRemove.setResult(bundle);
            } else {
                String strValueOf = String.valueOf(str);
                Log.w("FirebaseInstanceId", strValueOf.length() != 0 ? "Missing callback for ".concat(strValueOf) : new String("Missing callback for "));
            }
        }
    }

    private final void zzr(Intent intent) {
        String stringExtra = intent.getStringExtra("error");
        if (stringExtra == null) {
            String strValueOf = String.valueOf(intent.getExtras());
            StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 49);
            sb.append("Unexpected response, no error or registration id ");
            sb.append(strValueOf);
            Log.w("FirebaseInstanceId", sb.toString());
            return;
        }
        if (Log.isLoggable("FirebaseInstanceId", 3)) {
            String strValueOf2 = String.valueOf(stringExtra);
            Log.d("FirebaseInstanceId", strValueOf2.length() != 0 ? "Received InstanceID error ".concat(strValueOf2) : new String("Received InstanceID error "));
        }
        if (!stringExtra.startsWith("|")) {
            synchronized (this.zzolg) {
                for (int i = 0; i < this.zzolg.size(); i++) {
                    zzh(this.zzolg.keyAt(i), intent.getExtras());
                }
            }
            return;
        }
        String[] strArrSplit = stringExtra.split("\\|");
        if (strArrSplit.length <= 2 || !"ID".equals(strArrSplit[1])) {
            String strValueOf3 = String.valueOf(stringExtra);
            Log.w("FirebaseInstanceId", strValueOf3.length() != 0 ? "Unexpected structured response ".concat(strValueOf3) : new String("Unexpected structured response "));
            return;
        }
        String str = strArrSplit[2];
        String strSubstring = strArrSplit[3];
        if (strSubstring.startsWith(":")) {
            strSubstring = strSubstring.substring(1);
        }
        zzh(str, intent.putExtra("error", strSubstring).getExtras());
    }

    private final Bundle zzz(Bundle bundle) throws IOException {
        Bundle bundleZzaa = zzaa(bundle);
        if (bundleZzaa == null || !bundleZzaa.containsKey("google.messenger")) {
            return bundleZzaa;
        }
        Bundle bundleZzaa2 = zzaa(bundle);
        if (bundleZzaa2 == null || !bundleZzaa2.containsKey("google.messenger")) {
            return bundleZzaa2;
        }
        return null;
    }

    final Bundle zzah(Bundle bundle) throws IOException {
        if (this.zzokq.zzclo() < 12000000) {
            return zzz(bundle);
        }
        try {
            return (Bundle) Tasks.await(zzk.zzfa(this.zzaiq).zzj(1, bundle));
        } catch (InterruptedException | ExecutionException e) {
            if (Log.isLoggable("FirebaseInstanceId", 3)) {
                String strValueOf = String.valueOf(e);
                StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 22);
                sb.append("Error making request: ");
                sb.append(strValueOf);
                Log.d("FirebaseInstanceId", sb.toString());
            }
            if ((e.getCause() instanceof zzu) && ((zzu) e.getCause()).getErrorCode() == 4) {
                return zzz(bundle);
            }
            return null;
        }
    }
}
