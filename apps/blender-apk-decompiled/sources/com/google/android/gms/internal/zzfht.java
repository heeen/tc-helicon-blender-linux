package com.google.android.gms.internal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class zzfht implements zzfjb {
    private static final zzfht zzppf = new zzfht();
    private final Map<Class<?>, Method> zzppg = new HashMap();

    private zzfht() {
    }

    public static zzfht zzczp() {
        return zzppf;
    }

    @Override // com.google.android.gms.internal.zzfjb
    public final boolean zzi(Class<?> cls) {
        return zzfhu.class.isAssignableFrom(cls);
    }

    @Override // com.google.android.gms.internal.zzfjb
    public final zzfja zzj(Class<?> cls) {
        if (!zzfhu.class.isAssignableFrom(cls)) {
            String strValueOf = String.valueOf(cls.getName());
            throw new IllegalArgumentException(strValueOf.length() != 0 ? "Unsupported message type: ".concat(strValueOf) : new String("Unsupported message type: "));
        }
        try {
            Method declaredMethod = this.zzppg.get(cls);
            if (declaredMethod == null) {
                declaredMethod = cls.getDeclaredMethod("buildMessageInfo", new Class[0]);
                declaredMethod.setAccessible(true);
                this.zzppg.put(cls, declaredMethod);
            }
            return (zzfja) declaredMethod.invoke(null, new Object[0]);
        } catch (Exception e) {
            String strValueOf2 = String.valueOf(cls.getName());
            throw new RuntimeException(strValueOf2.length() != 0 ? "Unable to get message info for ".concat(strValueOf2) : new String("Unable to get message info for "), e);
        }
    }
}
