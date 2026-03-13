package android.support.constraint.solver.widgets;

import android.support.constraint.solver.LinearSystem;

/* JADX INFO: loaded from: classes.dex */
class Chain {
    private static final boolean DEBUG = false;

    Chain() {
    }

    static void applyChainConstraints(ConstraintWidgetContainer constraintWidgetContainer, LinearSystem linearSystem, int i) {
        int i2;
        int i3;
        ChainHead[] chainHeadArr;
        if (i == 0) {
            int i4 = constraintWidgetContainer.mHorizontalChainsSize;
            chainHeadArr = constraintWidgetContainer.mHorizontalChainsArray;
            i3 = i4;
            i2 = 0;
        } else {
            i2 = 2;
            i3 = constraintWidgetContainer.mVerticalChainsSize;
            chainHeadArr = constraintWidgetContainer.mVerticalChainsArray;
        }
        for (int i5 = 0; i5 < i3; i5++) {
            ChainHead chainHead = chainHeadArr[i5];
            chainHead.define();
            if (constraintWidgetContainer.optimizeFor(4)) {
                if (!Optimizer.applyChainOptimized(constraintWidgetContainer, linearSystem, i, i2, chainHead)) {
                    applyChainConstraints(constraintWidgetContainer, linearSystem, i, i2, chainHead);
                }
            } else {
                applyChainConstraints(constraintWidgetContainer, linearSystem, i, i2, chainHead);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x0037 A[PHI: r6 r8
      0x0037: PHI (r6v47 boolean) = (r6v3 boolean), (r6v50 boolean) binds: [B:30:0x0053, B:17:0x0035] A[DONT_GENERATE, DONT_INLINE]
      0x0037: PHI (r8v34 boolean) = (r8v1 boolean), (r8v37 boolean) binds: [B:30:0x0053, B:17:0x0035] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Removed duplicated region for block: B:19:0x0039 A[PHI: r6 r8
      0x0039: PHI (r6v5 boolean) = (r6v3 boolean), (r6v50 boolean) binds: [B:30:0x0053, B:17:0x0035] A[DONT_GENERATE, DONT_INLINE]
      0x0039: PHI (r8v3 boolean) = (r8v1 boolean), (r8v37 boolean) binds: [B:30:0x0053, B:17:0x0035] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Removed duplicated region for block: B:253:0x04b6 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:85:0x0161  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    static void applyChainConstraints(android.support.constraint.solver.widgets.ConstraintWidgetContainer r47, android.support.constraint.solver.LinearSystem r48, int r49, int r50, android.support.constraint.solver.widgets.ChainHead r51) {
        /*
            Method dump skipped, instruction units count: 1325
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.constraint.solver.widgets.Chain.applyChainConstraints(android.support.constraint.solver.widgets.ConstraintWidgetContainer, android.support.constraint.solver.LinearSystem, int, int, android.support.constraint.solver.widgets.ChainHead):void");
    }
}
