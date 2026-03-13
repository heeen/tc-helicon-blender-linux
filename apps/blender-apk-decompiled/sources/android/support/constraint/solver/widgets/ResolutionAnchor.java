package android.support.constraint.solver.widgets;

import android.support.constraint.solver.LinearSystem;
import android.support.constraint.solver.SolverVariable;
import android.support.constraint.solver.widgets.ConstraintAnchor;

/* JADX INFO: loaded from: classes.dex */
public class ResolutionAnchor extends ResolutionNode {
    public static final int BARRIER_CONNECTION = 5;
    public static final int CENTER_CONNECTION = 2;
    public static final int CHAIN_CONNECTION = 4;
    public static final int DIRECT_CONNECTION = 1;
    public static final int MATCH_CONNECTION = 3;
    public static final int UNCONNECTED = 0;
    float computedValue;
    ConstraintAnchor myAnchor;
    float offset;
    private ResolutionAnchor opposite;
    private float oppositeOffset;
    float resolvedOffset;
    ResolutionAnchor resolvedTarget;
    ResolutionAnchor target;
    int type = 0;
    private ResolutionDimension dimension = null;
    private int dimensionMultiplier = 1;
    private ResolutionDimension oppositeDimension = null;
    private int oppositeDimensionMultiplier = 1;

    String sType(int i) {
        return i == 1 ? "DIRECT" : i == 2 ? "CENTER" : i == 3 ? "MATCH" : i == 4 ? "CHAIN" : i == 5 ? "BARRIER" : "UNCONNECTED";
    }

    public ResolutionAnchor(ConstraintAnchor constraintAnchor) {
        this.myAnchor = constraintAnchor;
    }

    @Override // android.support.constraint.solver.widgets.ResolutionNode
    public void remove(ResolutionDimension resolutionDimension) {
        if (this.dimension == resolutionDimension) {
            this.dimension = null;
            this.offset = this.dimensionMultiplier;
        } else if (this.dimension == this.oppositeDimension) {
            this.oppositeDimension = null;
            this.oppositeOffset = this.oppositeDimensionMultiplier;
        }
        resolve();
    }

    public String toString() {
        if (this.state == 1) {
            if (this.resolvedTarget == this) {
                return "[" + this.myAnchor + ", RESOLVED: " + this.resolvedOffset + "]  type: " + sType(this.type);
            }
            return "[" + this.myAnchor + ", RESOLVED: " + this.resolvedTarget + ":" + this.resolvedOffset + "] type: " + sType(this.type);
        }
        return "{ " + this.myAnchor + " UNRESOLVED} type: " + sType(this.type);
    }

    public void resolve(ResolutionAnchor resolutionAnchor, float f) {
        if (this.state == 0 || !(this.resolvedTarget == resolutionAnchor || this.resolvedOffset == f)) {
            this.resolvedTarget = resolutionAnchor;
            this.resolvedOffset = f;
            if (this.state == 1) {
                invalidate();
            }
            didResolve();
        }
    }

    @Override // android.support.constraint.solver.widgets.ResolutionNode
    public void resolve() {
        float f;
        float width;
        float f2;
        boolean z = true;
        if (this.state == 1 || this.type == 4) {
            return;
        }
        if (this.dimension != null) {
            if (this.dimension.state != 1) {
                return;
            } else {
                this.offset = this.dimensionMultiplier * this.dimension.value;
            }
        }
        if (this.oppositeDimension != null) {
            if (this.oppositeDimension.state != 1) {
                return;
            } else {
                this.oppositeOffset = this.oppositeDimensionMultiplier * this.oppositeDimension.value;
            }
        }
        if (this.type == 1 && (this.target == null || this.target.state == 1)) {
            if (this.target == null) {
                this.resolvedTarget = this;
                this.resolvedOffset = this.offset;
            } else {
                this.resolvedTarget = this.target.resolvedTarget;
                this.resolvedOffset = this.target.resolvedOffset + this.offset;
            }
            didResolve();
            return;
        }
        if (this.type == 2 && this.target != null && this.target.state == 1 && this.opposite != null && this.opposite.target != null && this.opposite.target.state == 1) {
            if (LinearSystem.getMetrics() != null) {
                LinearSystem.getMetrics().centerConnectionResolved++;
            }
            this.resolvedTarget = this.target.resolvedTarget;
            this.opposite.resolvedTarget = this.opposite.target.resolvedTarget;
            int i = 0;
            if (this.myAnchor.mType != ConstraintAnchor.Type.RIGHT && this.myAnchor.mType != ConstraintAnchor.Type.BOTTOM) {
                z = false;
            }
            if (z) {
                f = this.target.resolvedOffset - this.opposite.target.resolvedOffset;
            } else {
                f = this.opposite.target.resolvedOffset - this.target.resolvedOffset;
            }
            if (this.myAnchor.mType == ConstraintAnchor.Type.LEFT || this.myAnchor.mType == ConstraintAnchor.Type.RIGHT) {
                width = f - this.myAnchor.mOwner.getWidth();
                f2 = this.myAnchor.mOwner.mHorizontalBiasPercent;
            } else {
                width = f - this.myAnchor.mOwner.getHeight();
                f2 = this.myAnchor.mOwner.mVerticalBiasPercent;
            }
            int margin = this.myAnchor.getMargin();
            int margin2 = this.opposite.myAnchor.getMargin();
            if (this.myAnchor.getTarget() == this.opposite.myAnchor.getTarget()) {
                f2 = 0.5f;
                margin2 = 0;
            } else {
                i = margin;
            }
            float f3 = i;
            float f4 = margin2;
            float f5 = (width - f3) - f4;
            if (z) {
                this.opposite.resolvedOffset = this.opposite.target.resolvedOffset + f4 + (f5 * f2);
                this.resolvedOffset = (this.target.resolvedOffset - f3) - (f5 * (1.0f - f2));
            } else {
                this.resolvedOffset = this.target.resolvedOffset + f3 + (f5 * f2);
                this.opposite.resolvedOffset = (this.opposite.target.resolvedOffset - f4) - (f5 * (1.0f - f2));
            }
            didResolve();
            this.opposite.didResolve();
            return;
        }
        if (this.type == 3 && this.target != null && this.target.state == 1 && this.opposite != null && this.opposite.target != null && this.opposite.target.state == 1) {
            if (LinearSystem.getMetrics() != null) {
                LinearSystem.getMetrics().matchConnectionResolved++;
            }
            this.resolvedTarget = this.target.resolvedTarget;
            this.opposite.resolvedTarget = this.opposite.target.resolvedTarget;
            this.resolvedOffset = this.target.resolvedOffset + this.offset;
            this.opposite.resolvedOffset = this.opposite.target.resolvedOffset + this.opposite.offset;
            didResolve();
            this.opposite.didResolve();
            return;
        }
        if (this.type == 5) {
            this.myAnchor.mOwner.resolve();
        }
    }

    public void setType(int i) {
        this.type = i;
    }

    @Override // android.support.constraint.solver.widgets.ResolutionNode
    public void reset() {
        super.reset();
        this.target = null;
        this.offset = 0.0f;
        this.dimension = null;
        this.dimensionMultiplier = 1;
        this.oppositeDimension = null;
        this.oppositeDimensionMultiplier = 1;
        this.resolvedTarget = null;
        this.resolvedOffset = 0.0f;
        this.computedValue = 0.0f;
        this.opposite = null;
        this.oppositeOffset = 0.0f;
        this.type = 0;
    }

    public void update() {
        ConstraintAnchor target = this.myAnchor.getTarget();
        if (target == null) {
            return;
        }
        if (target.getTarget() == this.myAnchor) {
            this.type = 4;
            target.getResolutionNode().type = 4;
        }
        int margin = this.myAnchor.getMargin();
        if (this.myAnchor.mType == ConstraintAnchor.Type.RIGHT || this.myAnchor.mType == ConstraintAnchor.Type.BOTTOM) {
            margin = -margin;
        }
        dependsOn(target.getResolutionNode(), margin);
    }

    public void dependsOn(int i, ResolutionAnchor resolutionAnchor, int i2) {
        this.type = i;
        this.target = resolutionAnchor;
        this.offset = i2;
        this.target.addDependent(this);
    }

    public void dependsOn(ResolutionAnchor resolutionAnchor, int i) {
        this.target = resolutionAnchor;
        this.offset = i;
        this.target.addDependent(this);
    }

    public void dependsOn(ResolutionAnchor resolutionAnchor, int i, ResolutionDimension resolutionDimension) {
        this.target = resolutionAnchor;
        this.target.addDependent(this);
        this.dimension = resolutionDimension;
        this.dimensionMultiplier = i;
        this.dimension.addDependent(this);
    }

    public void setOpposite(ResolutionAnchor resolutionAnchor, float f) {
        this.opposite = resolutionAnchor;
        this.oppositeOffset = f;
    }

    public void setOpposite(ResolutionAnchor resolutionAnchor, int i, ResolutionDimension resolutionDimension) {
        this.opposite = resolutionAnchor;
        this.oppositeDimension = resolutionDimension;
        this.oppositeDimensionMultiplier = i;
    }

    void addResolvedValue(LinearSystem linearSystem) {
        SolverVariable solverVariable = this.myAnchor.getSolverVariable();
        if (this.resolvedTarget == null) {
            linearSystem.addEquality(solverVariable, (int) this.resolvedOffset);
        } else {
            linearSystem.addEquality(solverVariable, linearSystem.createObjectVariable(this.resolvedTarget.myAnchor), (int) this.resolvedOffset, 6);
        }
    }

    public float getResolvedValue() {
        return this.resolvedOffset;
    }
}
