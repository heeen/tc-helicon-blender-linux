package com.google.android.gms.tasks;

import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
final class zzs implements Continuation<Void, List<Task<?>>> {
    private /* synthetic */ Collection zzles;

    zzs(Collection collection) {
        this.zzles = collection;
    }

    @Override // com.google.android.gms.tasks.Continuation
    public final /* synthetic */ List<Task<?>> then(@NonNull Task<Void> task) throws Exception {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.zzles);
        return arrayList;
    }
}
