package com.junitism.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-execution branch data: branchId -> (trueDistance, falseDistance, executed, outcome).
 */
public class BranchExecutionData {

    public static final class BranchRecord {
        public final double trueDistance;
        public final double falseDistance;
        public final boolean executed;
        public final boolean tookTrueBranch;

        BranchRecord(double trueDistance, double falseDistance, boolean tookTrueBranch) {
            this.trueDistance = trueDistance;
            this.falseDistance = falseDistance;
            this.executed = true;
            this.tookTrueBranch = tookTrueBranch;
        }
    }

    private final Map<Integer, BranchRecord> branches = new HashMap<>();
    private final Set<Integer> executedBranches = new HashSet<>();

    public void record(int branchId, String location, double trueDist, double falseDist, int val1, int val2) {
        executedBranches.add(branchId);
        boolean tookTrue = trueDist == 0;
        branches.put(branchId, new BranchRecord(trueDist, falseDist, tookTrue));
    }

    public boolean wasBranchExecuted(int branchId) {
        return executedBranches.contains(branchId);
    }

    public double getBranchDistance(int branchId, boolean wantTrue) {
        BranchRecord r = branches.get(branchId);
        if (r == null) return Double.MAX_VALUE;
        return wantTrue ? r.trueDistance : r.falseDistance;
    }

    public boolean tookTrueBranch(int branchId) {
        BranchRecord r = branches.get(branchId);
        return r != null && r.tookTrueBranch;
    }

    public Set<Integer> getExecutedBranches() {
        return new HashSet<>(executedBranches);
    }

    public Map<Integer, BranchRecord> getBranches() {
        return new HashMap<>(branches);
    }
}
