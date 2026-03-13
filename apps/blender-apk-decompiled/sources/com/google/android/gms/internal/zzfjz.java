package com.google.android.gms.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/* JADX INFO: Add missing generic type declarations: [FieldDescriptorType] */
/* JADX INFO: loaded from: classes.dex */
final class zzfjz<FieldDescriptorType> extends zzfjy<FieldDescriptorType, Object> {
    zzfjz(int i) {
        super(i, null);
    }

    @Override // com.google.android.gms.internal.zzfjy
    public final void zzbkr() {
        if (!isImmutable()) {
            for (int i = 0; i < zzdbp(); i++) {
                Map.Entry<FieldDescriptorType, Object> entryZzmr = zzmr(i);
                if (((zzfhs) entryZzmr.getKey()).zzczn()) {
                    entryZzmr.setValue(Collections.unmodifiableList((List) entryZzmr.getValue()));
                }
            }
            for (Map.Entry<FieldDescriptorType, Object> entry : zzdbq()) {
                if (((zzfhs) entry.getKey()).zzczn()) {
                    entry.setValue(Collections.unmodifiableList((List) entry.getValue()));
                }
            }
        }
        super.zzbkr();
    }
}
