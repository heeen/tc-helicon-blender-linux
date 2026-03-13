package com.google.android.gms.common.internal;

import android.accounts.Account;
import android.content.Context;
import android.view.View;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.internal.zzcyk;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
public final class zzr {
    private final String zzehh;
    private final Account zzeho;
    private final Set<Scope> zzfsw;
    private final int zzfsy;
    private final View zzfsz;
    private final String zzfta;
    private final Set<Scope> zzgfq;
    private final Map<Api<?>, zzt> zzgfr;
    private final zzcyk zzgfs;
    private Integer zzgft;

    @Hide
    public zzr(Account account, Set<Scope> set, Map<Api<?>, zzt> map, int i, View view, String str, String str2, zzcyk zzcykVar) {
        this.zzeho = account;
        this.zzfsw = set == null ? Collections.EMPTY_SET : Collections.unmodifiableSet(set);
        this.zzgfr = map == null ? Collections.EMPTY_MAP : map;
        this.zzfsz = view;
        this.zzfsy = i;
        this.zzehh = str;
        this.zzfta = str2;
        this.zzgfs = zzcykVar;
        HashSet hashSet = new HashSet(this.zzfsw);
        Iterator<zzt> it = this.zzgfr.values().iterator();
        while (it.hasNext()) {
            hashSet.addAll(it.next().zzenh);
        }
        this.zzgfq = Collections.unmodifiableSet(hashSet);
    }

    public static zzr zzcm(Context context) {
        return new GoogleApiClient.Builder(context).zzaic();
    }

    public final Account getAccount() {
        return this.zzeho;
    }

    @Deprecated
    public final String getAccountName() {
        if (this.zzeho != null) {
            return this.zzeho.name;
        }
        return null;
    }

    public final Account zzamd() {
        return this.zzeho != null ? this.zzeho : new Account("<<default account>>", "com.google");
    }

    public final int zzame() {
        return this.zzfsy;
    }

    public final Set<Scope> zzamf() {
        return this.zzfsw;
    }

    public final Set<Scope> zzamg() {
        return this.zzgfq;
    }

    public final Map<Api<?>, zzt> zzamh() {
        return this.zzgfr;
    }

    public final String zzami() {
        return this.zzehh;
    }

    public final String zzamj() {
        return this.zzfta;
    }

    public final View zzamk() {
        return this.zzfsz;
    }

    public final zzcyk zzaml() {
        return this.zzgfs;
    }

    public final Integer zzamm() {
        return this.zzgft;
    }

    public final Set<Scope> zzc(Api<?> api) {
        zzt zztVar = this.zzgfr.get(api);
        if (zztVar == null || zztVar.zzenh.isEmpty()) {
            return this.zzfsw;
        }
        HashSet hashSet = new HashSet(this.zzfsw);
        hashSet.addAll(zztVar.zzenh);
        return hashSet;
    }

    public final void zzc(Integer num) {
        this.zzgft = num;
    }
}
