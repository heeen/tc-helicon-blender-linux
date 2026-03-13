package android.support.constraint.solver.widgets;

import android.support.constraint.solver.LinearSystem;
import android.support.constraint.solver.SolverVariable;
import android.support.constraint.solver.widgets.ConstraintWidget;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class Barrier extends Helper {
    public static final int BOTTOM = 3;
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int TOP = 2;
    private int mBarrierType = 0;
    private ArrayList<ResolutionAnchor> mNodes = new ArrayList<>(4);
    private boolean mAllowsGoneWidget = true;

    @Override // android.support.constraint.solver.widgets.ConstraintWidget
    public boolean allowedInBarrier() {
        return true;
    }

    public void setBarrierType(int i) {
        this.mBarrierType = i;
    }

    public void setAllowsGoneWidget(boolean z) {
        this.mAllowsGoneWidget = z;
    }

    @Override // android.support.constraint.solver.widgets.ConstraintWidget
    public void resetResolutionNodes() {
        super.resetResolutionNodes();
        this.mNodes.clear();
    }

    @Override // android.support.constraint.solver.widgets.ConstraintWidget
    public void analyze(int i) {
        ResolutionAnchor resolutionNode;
        ResolutionAnchor resolutionNode2;
        if (this.mParent != null && ((ConstraintWidgetContainer) this.mParent).optimizeFor(2)) {
            switch (this.mBarrierType) {
                case 0:
                    resolutionNode = this.mLeft.getResolutionNode();
                    break;
                case 1:
                    resolutionNode = this.mRight.getResolutionNode();
                    break;
                case 2:
                    resolutionNode = this.mTop.getResolutionNode();
                    break;
                case 3:
                    resolutionNode = this.mBottom.getResolutionNode();
                    break;
                default:
                    return;
            }
            resolutionNode.setType(5);
            if (this.mBarrierType == 0 || this.mBarrierType == 1) {
                this.mTop.getResolutionNode().resolve(null, 0.0f);
                this.mBottom.getResolutionNode().resolve(null, 0.0f);
            } else {
                this.mLeft.getResolutionNode().resolve(null, 0.0f);
                this.mRight.getResolutionNode().resolve(null, 0.0f);
            }
            this.mNodes.clear();
            for (int i2 = 0; i2 < this.mWidgetsCount; i2++) {
                ConstraintWidget constraintWidget = this.mWidgets[i2];
                if (this.mAllowsGoneWidget || constraintWidget.allowedInBarrier()) {
                    switch (this.mBarrierType) {
                        case 0:
                            resolutionNode2 = constraintWidget.mLeft.getResolutionNode();
                            break;
                        case 1:
                            resolutionNode2 = constraintWidget.mRight.getResolutionNode();
                            break;
                        case 2:
                            resolutionNode2 = constraintWidget.mTop.getResolutionNode();
                            break;
                        case 3:
                            resolutionNode2 = constraintWidget.mBottom.getResolutionNode();
                            break;
                        default:
                            resolutionNode2 = null;
                            break;
                    }
                    if (resolutionNode2 != null) {
                        this.mNodes.add(resolutionNode2);
                        resolutionNode2.addDependent(resolutionNode);
                    }
                }
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:12:0x0030  */
    /* JADX WARN: Removed duplicated region for block: B:29:0x0066  */
    /* JADX WARN: Removed duplicated region for block: B:33:0x007e  */
    /* JADX WARN: Removed duplicated region for block: B:34:0x0088  */
    /* JADX WARN: Removed duplicated region for block: B:35:0x0092  */
    /* JADX WARN: Removed duplicated region for block: B:36:0x009c  */
    /*  JADX ERROR: UnsupportedOperationException in pass: RegionMakerVisitor
        java.lang.UnsupportedOperationException
        	at java.base/java.util.Collections$UnmodifiableCollection.add(Collections.java:1092)
        	at jadx.core.dex.visitors.regions.maker.SwitchRegionMaker$1.leaveRegion(SwitchRegionMaker.java:390)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:70)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:68)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:68)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverse(DepthRegionTraversal.java:23)
        	at jadx.core.dex.visitors.regions.maker.SwitchRegionMaker.insertBreaksForCase(SwitchRegionMaker.java:370)
        	at jadx.core.dex.visitors.regions.maker.SwitchRegionMaker.insertBreaks(SwitchRegionMaker.java:85)
        	at jadx.core.dex.visitors.regions.PostProcessRegions.leaveRegion(PostProcessRegions.java:33)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:70)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:68)
        	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:68)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverse(DepthRegionTraversal.java:19)
        	at jadx.core.dex.visitors.regions.PostProcessRegions.process(PostProcessRegions.java:23)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:31)
        */
    @Override // android.support.constraint.solver.widgets.ConstraintWidget
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void resolve() {
        /*
            r8 = this;
            int r0 = r8.mBarrierType
            r1 = 2139095039(0x7f7fffff, float:3.4028235E38)
            r2 = 0
            switch(r0) {
                case 0: goto L20;
                case 1: goto L18;
                case 2: goto L11;
                case 3: goto La;
                default: goto L9;
            }
        L9:
            return
        La:
            android.support.constraint.solver.widgets.ConstraintAnchor r0 = r8.mBottom
            android.support.constraint.solver.widgets.ResolutionAnchor r0 = r0.getResolutionNode()
            goto L1e
        L11:
            android.support.constraint.solver.widgets.ConstraintAnchor r0 = r8.mTop
            android.support.constraint.solver.widgets.ResolutionAnchor r0 = r0.getResolutionNode()
            goto L26
        L18:
            android.support.constraint.solver.widgets.ConstraintAnchor r0 = r8.mRight
            android.support.constraint.solver.widgets.ResolutionAnchor r0 = r0.getResolutionNode()
        L1e:
            r1 = r2
            goto L26
        L20:
            android.support.constraint.solver.widgets.ConstraintAnchor r0 = r8.mLeft
            android.support.constraint.solver.widgets.ResolutionAnchor r0 = r0.getResolutionNode()
        L26:
            java.util.ArrayList<android.support.constraint.solver.widgets.ResolutionAnchor> r2 = r8.mNodes
            int r2 = r2.size()
            r3 = 0
            r4 = 0
        L2e:
            if (r4 >= r2) goto L60
            java.util.ArrayList<android.support.constraint.solver.widgets.ResolutionAnchor> r5 = r8.mNodes
            java.lang.Object r5 = r5.get(r4)
            android.support.constraint.solver.widgets.ResolutionAnchor r5 = (android.support.constraint.solver.widgets.ResolutionAnchor) r5
            int r6 = r5.state
            r7 = 1
            if (r6 == r7) goto L3e
            return
        L3e:
            int r6 = r8.mBarrierType
            if (r6 == 0) goto L53
            int r6 = r8.mBarrierType
            r7 = 2
            if (r6 != r7) goto L48
            goto L53
        L48:
            float r6 = r5.resolvedOffset
            int r6 = (r6 > r1 ? 1 : (r6 == r1 ? 0 : -1))
            if (r6 <= 0) goto L5d
            float r1 = r5.resolvedOffset
            android.support.constraint.solver.widgets.ResolutionAnchor r3 = r5.resolvedTarget
            goto L5d
        L53:
            float r6 = r5.resolvedOffset
            int r6 = (r6 > r1 ? 1 : (r6 == r1 ? 0 : -1))
            if (r6 >= 0) goto L5d
            float r1 = r5.resolvedOffset
            android.support.constraint.solver.widgets.ResolutionAnchor r3 = r5.resolvedTarget
        L5d:
            int r4 = r4 + 1
            goto L2e
        L60:
            android.support.constraint.solver.Metrics r2 = android.support.constraint.solver.LinearSystem.getMetrics()
            if (r2 == 0) goto L71
            android.support.constraint.solver.Metrics r2 = android.support.constraint.solver.LinearSystem.getMetrics()
            long r4 = r2.barrierConnectionResolved
            r6 = 1
            long r4 = r4 + r6
            r2.barrierConnectionResolved = r4
        L71:
            r0.resolvedTarget = r3
            r0.resolvedOffset = r1
            r0.didResolve()
            int r0 = r8.mBarrierType
            switch(r0) {
                case 0: goto L9c;
                case 1: goto L92;
                case 2: goto L88;
                case 3: goto L7e;
                default: goto L7d;
            }
        L7d:
            return
        L7e:
            android.support.constraint.solver.widgets.ConstraintAnchor r8 = r8.mTop
            android.support.constraint.solver.widgets.ResolutionAnchor r8 = r8.getResolutionNode()
            r8.resolve(r3, r1)
            goto La5
        L88:
            android.support.constraint.solver.widgets.ConstraintAnchor r8 = r8.mBottom
            android.support.constraint.solver.widgets.ResolutionAnchor r8 = r8.getResolutionNode()
            r8.resolve(r3, r1)
            goto La5
        L92:
            android.support.constraint.solver.widgets.ConstraintAnchor r8 = r8.mLeft
            android.support.constraint.solver.widgets.ResolutionAnchor r8 = r8.getResolutionNode()
            r8.resolve(r3, r1)
            goto La5
        L9c:
            android.support.constraint.solver.widgets.ConstraintAnchor r8 = r8.mRight
            android.support.constraint.solver.widgets.ResolutionAnchor r8 = r8.getResolutionNode()
            r8.resolve(r3, r1)
        La5:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.constraint.solver.widgets.Barrier.resolve():void");
    }

    @Override // android.support.constraint.solver.widgets.ConstraintWidget
    public void addToSolver(LinearSystem linearSystem) {
        boolean z;
        this.mListAnchors[0] = this.mLeft;
        this.mListAnchors[2] = this.mTop;
        this.mListAnchors[1] = this.mRight;
        this.mListAnchors[3] = this.mBottom;
        for (int i = 0; i < this.mListAnchors.length; i++) {
            this.mListAnchors[i].mSolverVariable = linearSystem.createObjectVariable(this.mListAnchors[i]);
        }
        if (this.mBarrierType < 0 || this.mBarrierType >= 4) {
            return;
        }
        ConstraintAnchor constraintAnchor = this.mListAnchors[this.mBarrierType];
        for (int i2 = 0; i2 < this.mWidgetsCount; i2++) {
            ConstraintWidget constraintWidget = this.mWidgets[i2];
            if ((this.mAllowsGoneWidget || constraintWidget.allowedInBarrier()) && (((this.mBarrierType == 0 || this.mBarrierType == 1) && constraintWidget.getHorizontalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) || ((this.mBarrierType == 2 || this.mBarrierType == 3) && constraintWidget.getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT))) {
                z = true;
                break;
            }
        }
        z = false;
        if (this.mBarrierType == 0 || this.mBarrierType == 1 ? getParent().getHorizontalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT : getParent().getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
            z = false;
        }
        for (int i3 = 0; i3 < this.mWidgetsCount; i3++) {
            ConstraintWidget constraintWidget2 = this.mWidgets[i3];
            if (this.mAllowsGoneWidget || constraintWidget2.allowedInBarrier()) {
                SolverVariable solverVariableCreateObjectVariable = linearSystem.createObjectVariable(constraintWidget2.mListAnchors[this.mBarrierType]);
                constraintWidget2.mListAnchors[this.mBarrierType].mSolverVariable = solverVariableCreateObjectVariable;
                if (this.mBarrierType == 0 || this.mBarrierType == 2) {
                    linearSystem.addLowerBarrier(constraintAnchor.mSolverVariable, solverVariableCreateObjectVariable, z);
                } else {
                    linearSystem.addGreaterBarrier(constraintAnchor.mSolverVariable, solverVariableCreateObjectVariable, z);
                }
            }
        }
        if (this.mBarrierType == 0) {
            linearSystem.addEquality(this.mRight.mSolverVariable, this.mLeft.mSolverVariable, 0, 6);
            if (z) {
                return;
            }
            linearSystem.addEquality(this.mLeft.mSolverVariable, this.mParent.mRight.mSolverVariable, 0, 5);
            return;
        }
        if (this.mBarrierType == 1) {
            linearSystem.addEquality(this.mLeft.mSolverVariable, this.mRight.mSolverVariable, 0, 6);
            if (z) {
                return;
            }
            linearSystem.addEquality(this.mLeft.mSolverVariable, this.mParent.mLeft.mSolverVariable, 0, 5);
            return;
        }
        if (this.mBarrierType == 2) {
            linearSystem.addEquality(this.mBottom.mSolverVariable, this.mTop.mSolverVariable, 0, 6);
            if (z) {
                return;
            }
            linearSystem.addEquality(this.mTop.mSolverVariable, this.mParent.mBottom.mSolverVariable, 0, 5);
            return;
        }
        if (this.mBarrierType == 3) {
            linearSystem.addEquality(this.mTop.mSolverVariable, this.mBottom.mSolverVariable, 0, 6);
            if (z) {
                return;
            }
            linearSystem.addEquality(this.mTop.mSolverVariable, this.mParent.mTop.mSolverVariable, 0, 5);
        }
    }
}
