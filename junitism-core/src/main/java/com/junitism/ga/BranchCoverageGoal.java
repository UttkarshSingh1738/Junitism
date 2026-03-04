package com.junitism.ga;

import java.util.Objects;

/**
 * Single branch coverage objective: branchId + true/false side.
 */
public final class BranchCoverageGoal {

    private final int branchId;
    private final boolean wantTrue;
    private final String location;

    public BranchCoverageGoal(int branchId, boolean wantTrue) {
        this(branchId, wantTrue, null);
    }

    public BranchCoverageGoal(int branchId, boolean wantTrue, String location) {
        this.branchId = branchId;
        this.wantTrue = wantTrue;
        this.location = location;
    }

    public int getBranchId() {
        return branchId;
    }

    public boolean isTrue() {
        return wantTrue;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BranchCoverageGoal that = (BranchCoverageGoal) o;
        return branchId == that.branchId && wantTrue == that.wantTrue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchId, wantTrue);
    }

    @Override
    public String toString() {
        return "Branch" + branchId + (wantTrue ? "T" : "F");
    }
}
