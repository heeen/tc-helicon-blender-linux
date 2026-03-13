package com.google.android.gms.auth.api.signin;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.google.android.gms.auth.api.signin.internal.zzo;
import com.google.android.gms.auth.api.signin.internal.zzq;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.Hide;
import com.google.android.gms.common.internal.ReflectedParcelable;
import com.google.android.gms.common.internal.zzbq;
import com.google.android.gms.internal.zzbgl;
import com.google.android.gms.internal.zzbgo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class GoogleSignInOptions extends zzbgl implements Api.ApiOptions.Optional, ReflectedParcelable {
    private int versionCode;
    private Account zzeho;
    private boolean zzela;
    private String zzelb;
    private final ArrayList<Scope> zzena;
    private final boolean zzenb;
    private final boolean zzenc;
    private String zzend;
    private ArrayList<zzo> zzene;
    private Map<Integer, zzo> zzenf;

    @Hide
    public static final Scope zzemx = new Scope(Scopes.PROFILE);

    @Hide
    public static final Scope zzemy = new Scope("email");

    @Hide
    public static final Scope zzemz = new Scope("openid");

    @Hide
    public static final Scope SCOPE_GAMES_LITE = new Scope("https://www.googleapis.com/auth/games_lite");

    @Hide
    public static final Scope SCOPE_GAMES = new Scope(Scopes.GAMES);
    public static final GoogleSignInOptions DEFAULT_SIGN_IN = new Builder().requestId().requestProfile().build();
    public static final GoogleSignInOptions DEFAULT_GAMES_SIGN_IN = new Builder().requestScopes(SCOPE_GAMES_LITE, new Scope[0]).build();
    public static final Parcelable.Creator<GoogleSignInOptions> CREATOR = new zze();
    private static Comparator<Scope> zzeng = new zzd();

    public static final class Builder {
        private Account zzeho;
        private boolean zzela;
        private String zzelb;
        private boolean zzenb;
        private boolean zzenc;
        private String zzend;
        private Set<Scope> zzenh;
        private Map<Integer, zzo> zzeni;

        public Builder() {
            this.zzenh = new HashSet();
            this.zzeni = new HashMap();
        }

        public Builder(@NonNull GoogleSignInOptions googleSignInOptions) {
            this.zzenh = new HashSet();
            this.zzeni = new HashMap();
            zzbq.checkNotNull(googleSignInOptions);
            this.zzenh = new HashSet(googleSignInOptions.zzena);
            this.zzenb = googleSignInOptions.zzenb;
            this.zzenc = googleSignInOptions.zzenc;
            this.zzela = googleSignInOptions.zzela;
            this.zzelb = googleSignInOptions.zzelb;
            this.zzeho = googleSignInOptions.zzeho;
            this.zzend = googleSignInOptions.zzend;
            this.zzeni = GoogleSignInOptions.zzx(googleSignInOptions.zzene);
        }

        private final String zzfc(String str) {
            zzbq.zzgv(str);
            zzbq.checkArgument(this.zzelb == null || this.zzelb.equals(str), "two different server client ids provided");
            return str;
        }

        public final Builder addExtension(GoogleSignInOptionsExtension googleSignInOptionsExtension) {
            if (this.zzeni.containsKey(Integer.valueOf(googleSignInOptionsExtension.getExtensionType()))) {
                throw new IllegalStateException("Only one extension per type may be added");
            }
            if (googleSignInOptionsExtension.getImpliedScopes() != null) {
                this.zzenh.addAll(googleSignInOptionsExtension.getImpliedScopes());
            }
            this.zzeni.put(Integer.valueOf(googleSignInOptionsExtension.getExtensionType()), new zzo(googleSignInOptionsExtension));
            return this;
        }

        public final GoogleSignInOptions build() {
            if (this.zzenh.contains(GoogleSignInOptions.SCOPE_GAMES) && this.zzenh.contains(GoogleSignInOptions.SCOPE_GAMES_LITE)) {
                this.zzenh.remove(GoogleSignInOptions.SCOPE_GAMES_LITE);
            }
            if (this.zzela && (this.zzeho == null || !this.zzenh.isEmpty())) {
                requestId();
            }
            return new GoogleSignInOptions(3, new ArrayList(this.zzenh), this.zzeho, this.zzela, this.zzenb, this.zzenc, this.zzelb, this.zzend, this.zzeni, null);
        }

        public final Builder requestEmail() {
            this.zzenh.add(GoogleSignInOptions.zzemy);
            return this;
        }

        public final Builder requestId() {
            this.zzenh.add(GoogleSignInOptions.zzemz);
            return this;
        }

        public final Builder requestIdToken(String str) {
            this.zzela = true;
            this.zzelb = zzfc(str);
            return this;
        }

        public final Builder requestProfile() {
            this.zzenh.add(GoogleSignInOptions.zzemx);
            return this;
        }

        public final Builder requestScopes(Scope scope, Scope... scopeArr) {
            this.zzenh.add(scope);
            this.zzenh.addAll(Arrays.asList(scopeArr));
            return this;
        }

        public final Builder requestServerAuthCode(String str) {
            return requestServerAuthCode(str, false);
        }

        public final Builder requestServerAuthCode(String str, boolean z) {
            this.zzenb = true;
            this.zzelb = zzfc(str);
            this.zzenc = z;
            return this;
        }

        public final Builder setAccountName(String str) {
            this.zzeho = new Account(zzbq.zzgv(str), "com.google");
            return this;
        }

        public final Builder setHostedDomain(String str) {
            this.zzend = zzbq.zzgv(str);
            return this;
        }
    }

    GoogleSignInOptions(int i, ArrayList<Scope> arrayList, Account account, boolean z, boolean z2, boolean z3, String str, String str2, ArrayList<zzo> arrayList2) {
        this(i, arrayList, account, z, z2, z3, str, str2, zzx(arrayList2));
    }

    private GoogleSignInOptions(int i, ArrayList<Scope> arrayList, Account account, boolean z, boolean z2, boolean z3, String str, String str2, Map<Integer, zzo> map) {
        this.versionCode = i;
        this.zzena = arrayList;
        this.zzeho = account;
        this.zzela = z;
        this.zzenb = z2;
        this.zzenc = z3;
        this.zzelb = str;
        this.zzend = str2;
        this.zzene = new ArrayList<>(map.values());
        this.zzenf = map;
    }

    /* synthetic */ GoogleSignInOptions(int i, ArrayList arrayList, Account account, boolean z, boolean z2, boolean z3, String str, String str2, Map map, zzd zzdVar) {
        this(3, (ArrayList<Scope>) arrayList, account, z, z2, z3, str, str2, (Map<Integer, zzo>) map);
    }

    private final JSONObject toJsonObject() {
        JSONObject jSONObject = new JSONObject();
        try {
            JSONArray jSONArray = new JSONArray();
            Collections.sort(this.zzena, zzeng);
            ArrayList<Scope> arrayList = this.zzena;
            int size = arrayList.size();
            int i = 0;
            while (i < size) {
                Scope scope = arrayList.get(i);
                i++;
                jSONArray.put(scope.zzaie());
            }
            jSONObject.put("scopes", jSONArray);
            if (this.zzeho != null) {
                jSONObject.put("accountName", this.zzeho.name);
            }
            jSONObject.put("idTokenRequested", this.zzela);
            jSONObject.put("forceCodeForRefreshToken", this.zzenc);
            jSONObject.put("serverAuthRequested", this.zzenb);
            if (!TextUtils.isEmpty(this.zzelb)) {
                jSONObject.put("serverClientId", this.zzelb);
            }
            if (!TextUtils.isEmpty(this.zzend)) {
                jSONObject.put("hostedDomain", this.zzend);
            }
            return jSONObject;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Hide
    @Nullable
    public static GoogleSignInOptions zzfb(@Nullable String str) throws JSONException {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        JSONObject jSONObject = new JSONObject(str);
        HashSet hashSet = new HashSet();
        JSONArray jSONArray = jSONObject.getJSONArray("scopes");
        int length = jSONArray.length();
        for (int i = 0; i < length; i++) {
            hashSet.add(new Scope(jSONArray.getString(i)));
        }
        String strOptString = jSONObject.optString("accountName", null);
        return new GoogleSignInOptions(3, (ArrayList<Scope>) new ArrayList(hashSet), !TextUtils.isEmpty(strOptString) ? new Account(strOptString, "com.google") : null, jSONObject.getBoolean("idTokenRequested"), jSONObject.getBoolean("serverAuthRequested"), jSONObject.getBoolean("forceCodeForRefreshToken"), jSONObject.optString("serverClientId", null), jSONObject.optString("hostedDomain", null), new HashMap());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Map<Integer, zzo> zzx(@Nullable List<zzo> list) {
        HashMap map = new HashMap();
        if (list == null) {
            return map;
        }
        for (zzo zzoVar : list) {
            map.put(Integer.valueOf(zzoVar.getType()), zzoVar);
        }
        return map;
    }

    /* JADX WARN: Removed duplicated region for block: B:24:0x004f A[Catch: ClassCastException -> 0x0076, TryCatch #0 {ClassCastException -> 0x0076, blocks: (B:5:0x0004, B:7:0x000e, B:10:0x0017, B:12:0x0027, B:15:0x0034, B:17:0x0038, B:22:0x0047, B:24:0x004f, B:29:0x0062, B:31:0x0068, B:33:0x006e, B:27:0x0058, B:20:0x003d), top: B:39:0x0004 }] */
    /* JADX WARN: Removed duplicated region for block: B:27:0x0058 A[Catch: ClassCastException -> 0x0076, TryCatch #0 {ClassCastException -> 0x0076, blocks: (B:5:0x0004, B:7:0x000e, B:10:0x0017, B:12:0x0027, B:15:0x0034, B:17:0x0038, B:22:0x0047, B:24:0x004f, B:29:0x0062, B:31:0x0068, B:33:0x006e, B:27:0x0058, B:20:0x003d), top: B:39:0x0004 }] */
    /* JADX WARN: Removed duplicated region for block: B:35:0x0074 A[RETURN] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean equals(java.lang.Object r4) {
        /*
            r3 = this;
            r0 = 0
            if (r4 != 0) goto L4
            return r0
        L4:
            com.google.android.gms.auth.api.signin.GoogleSignInOptions r4 = (com.google.android.gms.auth.api.signin.GoogleSignInOptions) r4     // Catch: java.lang.ClassCastException -> L76
            java.util.ArrayList<com.google.android.gms.auth.api.signin.internal.zzo> r1 = r3.zzene     // Catch: java.lang.ClassCastException -> L76
            int r1 = r1.size()     // Catch: java.lang.ClassCastException -> L76
            if (r1 > 0) goto L76
            java.util.ArrayList<com.google.android.gms.auth.api.signin.internal.zzo> r1 = r4.zzene     // Catch: java.lang.ClassCastException -> L76
            int r1 = r1.size()     // Catch: java.lang.ClassCastException -> L76
            if (r1 <= 0) goto L17
            goto L76
        L17:
            java.util.ArrayList<com.google.android.gms.common.api.Scope> r1 = r3.zzena     // Catch: java.lang.ClassCastException -> L76
            int r1 = r1.size()     // Catch: java.lang.ClassCastException -> L76
            java.util.ArrayList r2 = r4.zzaci()     // Catch: java.lang.ClassCastException -> L76
            int r2 = r2.size()     // Catch: java.lang.ClassCastException -> L76
            if (r1 != r2) goto L76
            java.util.ArrayList<com.google.android.gms.common.api.Scope> r1 = r3.zzena     // Catch: java.lang.ClassCastException -> L76
            java.util.ArrayList r2 = r4.zzaci()     // Catch: java.lang.ClassCastException -> L76
            boolean r1 = r1.containsAll(r2)     // Catch: java.lang.ClassCastException -> L76
            if (r1 != 0) goto L34
            goto L76
        L34:
            android.accounts.Account r1 = r3.zzeho     // Catch: java.lang.ClassCastException -> L76
            if (r1 != 0) goto L3d
            android.accounts.Account r1 = r4.zzeho     // Catch: java.lang.ClassCastException -> L76
            if (r1 != 0) goto L76
            goto L47
        L3d:
            android.accounts.Account r1 = r3.zzeho     // Catch: java.lang.ClassCastException -> L76
            android.accounts.Account r2 = r4.zzeho     // Catch: java.lang.ClassCastException -> L76
            boolean r1 = r1.equals(r2)     // Catch: java.lang.ClassCastException -> L76
            if (r1 == 0) goto L76
        L47:
            java.lang.String r1 = r3.zzelb     // Catch: java.lang.ClassCastException -> L76
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch: java.lang.ClassCastException -> L76
            if (r1 == 0) goto L58
            java.lang.String r1 = r4.zzelb     // Catch: java.lang.ClassCastException -> L76
            boolean r1 = android.text.TextUtils.isEmpty(r1)     // Catch: java.lang.ClassCastException -> L76
            if (r1 == 0) goto L76
            goto L62
        L58:
            java.lang.String r1 = r3.zzelb     // Catch: java.lang.ClassCastException -> L76
            java.lang.String r2 = r4.zzelb     // Catch: java.lang.ClassCastException -> L76
            boolean r1 = r1.equals(r2)     // Catch: java.lang.ClassCastException -> L76
            if (r1 == 0) goto L76
        L62:
            boolean r1 = r3.zzenc     // Catch: java.lang.ClassCastException -> L76
            boolean r2 = r4.zzenc     // Catch: java.lang.ClassCastException -> L76
            if (r1 != r2) goto L76
            boolean r1 = r3.zzela     // Catch: java.lang.ClassCastException -> L76
            boolean r2 = r4.zzela     // Catch: java.lang.ClassCastException -> L76
            if (r1 != r2) goto L76
            boolean r3 = r3.zzenb     // Catch: java.lang.ClassCastException -> L76
            boolean r4 = r4.zzenb     // Catch: java.lang.ClassCastException -> L76
            if (r3 != r4) goto L76
            r3 = 1
            return r3
        L76:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.gms.auth.api.signin.GoogleSignInOptions.equals(java.lang.Object):boolean");
    }

    @Hide
    public final Account getAccount() {
        return this.zzeho;
    }

    public Scope[] getScopeArray() {
        return (Scope[]) this.zzena.toArray(new Scope[this.zzena.size()]);
    }

    @Hide
    public final String getServerClientId() {
        return this.zzelb;
    }

    public int hashCode() {
        ArrayList arrayList = new ArrayList();
        ArrayList<Scope> arrayList2 = this.zzena;
        int size = arrayList2.size();
        int i = 0;
        while (i < size) {
            Scope scope = arrayList2.get(i);
            i++;
            arrayList.add(scope.zzaie());
        }
        Collections.sort(arrayList);
        return new zzq().zzs(arrayList).zzs(this.zzeho).zzs(this.zzelb).zzav(this.zzenc).zzav(this.zzela).zzav(this.zzenb).zzacr();
    }

    @Hide
    public final boolean isIdTokenRequested() {
        return this.zzela;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        int iZze = zzbgo.zze(parcel);
        zzbgo.zzc(parcel, 1, this.versionCode);
        zzbgo.zzc(parcel, 2, zzaci(), false);
        zzbgo.zza(parcel, 3, (Parcelable) this.zzeho, i, false);
        zzbgo.zza(parcel, 4, this.zzela);
        zzbgo.zza(parcel, 5, this.zzenb);
        zzbgo.zza(parcel, 6, this.zzenc);
        zzbgo.zza(parcel, 7, this.zzelb, false);
        zzbgo.zza(parcel, 8, this.zzend, false);
        zzbgo.zzc(parcel, 9, this.zzene, false);
        zzbgo.zzai(parcel, iZze);
    }

    @Hide
    public final ArrayList<Scope> zzaci() {
        return new ArrayList<>(this.zzena);
    }

    @Hide
    public final boolean zzacj() {
        return this.zzenb;
    }

    @Hide
    public final String zzack() {
        return toJsonObject().toString();
    }
}
