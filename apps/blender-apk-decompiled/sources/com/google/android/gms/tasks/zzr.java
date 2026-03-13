package com.google.android.gms.tasks;

import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: Add missing generic type declarations: [TResult] */
/* JADX INFO: loaded from: classes.dex */
final class zzr<TResult> implements Continuation<Void, List<TResult>> {
    private /* synthetic */ Collection zzles;

    zzr(Collection collection) {
        this.zzles = collection;
    }

    @Override // com.google.android.gms.tasks.Continuation
    public final /* synthetic */ Object then(@NonNull Task<Void> task) throws Exception {
        if (this.zzles.size() == 0) {
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList();
        Iterator it = this.zzles.iterator();
        while (it.hasNext()) {
            arrayList.add(((Task) it.next()).getResult());
        }
        return arrayList;
    }
}
