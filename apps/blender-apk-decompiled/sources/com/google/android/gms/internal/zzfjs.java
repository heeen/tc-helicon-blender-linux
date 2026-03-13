package com.google.android.gms.internal;

import java.util.Arrays;
import java.util.Stack;

/* JADX INFO: loaded from: classes.dex */
final class zzfjs {
    private final Stack<zzfgs> zzprx;

    private zzfjs() {
        this.zzprx = new Stack<>();
    }

    private final void zzbb(zzfgs zzfgsVar) {
        zzfjr zzfjrVar;
        while (!zzfgsVar.zzcxs()) {
            if (!(zzfgsVar instanceof zzfjq)) {
                String strValueOf = String.valueOf(zzfgsVar.getClass());
                StringBuilder sb = new StringBuilder(String.valueOf(strValueOf).length() + 49);
                sb.append("Has a new type of ByteString been created? Found ");
                sb.append(strValueOf);
                throw new IllegalArgumentException(sb.toString());
            }
            zzfjq zzfjqVar = (zzfjq) zzfgsVar;
            zzbb(zzfjqVar.zzprt);
            zzfgsVar = zzfjqVar.zzpru;
        }
        int iZzmp = zzmp(zzfgsVar.size());
        int i = zzfjq.zzprr[iZzmp + 1];
        if (this.zzprx.isEmpty() || this.zzprx.peek().size() >= i) {
            this.zzprx.push(zzfgsVar);
            return;
        }
        int i2 = zzfjq.zzprr[iZzmp];
        zzfgs zzfgsVarPop = this.zzprx.pop();
        while (true) {
            zzfjrVar = null;
            if (this.zzprx.isEmpty() || this.zzprx.peek().size() >= i2) {
                break;
            } else {
                zzfgsVarPop = new zzfjq(this.zzprx.pop(), zzfgsVarPop);
            }
        }
        zzfjq zzfjqVar2 = new zzfjq(zzfgsVarPop, zzfgsVar);
        while (!this.zzprx.isEmpty()) {
            if (this.zzprx.peek().size() >= zzfjq.zzprr[zzmp(zzfjqVar2.size()) + 1]) {
                break;
            } else {
                zzfjqVar2 = new zzfjq(this.zzprx.pop(), zzfjqVar2);
            }
        }
        this.zzprx.push(zzfjqVar2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final zzfgs zzc(zzfgs zzfgsVar, zzfgs zzfgsVar2) {
        zzbb(zzfgsVar);
        zzbb(zzfgsVar2);
        zzfgs zzfgsVarPop = this.zzprx.pop();
        while (!this.zzprx.isEmpty()) {
            zzfgsVarPop = new zzfjq(this.zzprx.pop(), zzfgsVarPop);
        }
        return zzfgsVarPop;
    }

    private static int zzmp(int i) {
        int iBinarySearch = Arrays.binarySearch(zzfjq.zzprr, i);
        return iBinarySearch < 0 ? (-(iBinarySearch + 1)) - 1 : iBinarySearch;
    }
}
