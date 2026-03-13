package com.google.android.gms.internal;

import com.google.android.gms.internal.zzfjc;

/* JADX INFO: loaded from: classes.dex */
public abstract class zzfgm<MessageType extends zzfjc> implements zzfjl<MessageType> {
    private static final zzfhm zzpns = zzfhm.zzczf();

    @Override // com.google.android.gms.internal.zzfjl
    public final /* synthetic */ Object zzc(zzfhb zzfhbVar, zzfhm zzfhmVar) throws zzfie {
        MessageType messagetypeZze = zze(zzfhbVar, zzfhmVar);
        if (messagetypeZze == null || messagetypeZze.isInitialized()) {
            return messagetypeZze;
        }
        throw (!(messagetypeZze instanceof zzfgj) ? messagetypeZze instanceof zzfgl ? new zzfkm((zzfgl) messagetypeZze) : new zzfkm(messagetypeZze) : new zzfkm((zzfgj) messagetypeZze)).zzdbz().zzi(messagetypeZze);
    }
}
