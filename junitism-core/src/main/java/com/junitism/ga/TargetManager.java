package com.junitism.ga;

import com.junitism.instrumentation.ControlDependencyGraph;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dynamic target activation via CDG.
 */
public class TargetManager {

    private final ControlDependencyGraph cdg;
    private final Set<BranchCoverageGoal> allGoals;
    private final Set<BranchCoverageGoal> currentGoals = new HashSet<>();
    private final Set<BranchCoverageGoal> coveredGoals = new HashSet<>();

    public TargetManager(ControlDependencyGraph cdg, Set<BranchCoverageGoal> allGoals) {
        this.cdg = cdg;
        this.allGoals = new HashSet<>(allGoals);
        initialize();
    }

    private void initialize() {
        for (Integer branchId : cdg.getRootBranches()) {
            currentGoals.add(new BranchCoverageGoal(branchId, true));
            currentGoals.add(new BranchCoverageGoal(branchId, false));
        }
    }

    public void updateAfterCoverage(Set<BranchCoverageGoal> newlyCovered) {
        for (BranchCoverageGoal goal : newlyCovered) {
            coveredGoals.add(goal);
            for (Integer childId : cdg.getStructuralChildren(goal.getBranchId())) {
                BranchCoverageGoal childTrue = new BranchCoverageGoal(childId, true);
                BranchCoverageGoal childFalse = new BranchCoverageGoal(childId, false);
                if (!coveredGoals.contains(childTrue)) currentGoals.add(childTrue);
                if (!coveredGoals.contains(childFalse)) currentGoals.add(childFalse);
            }
        }
    }

    public Set<BranchCoverageGoal> getCurrentGoals() {
        return new HashSet<>(currentGoals);
    }

    public Set<BranchCoverageGoal> getAllGoals() {
        return new HashSet<>(allGoals);
    }

    public Set<BranchCoverageGoal> getCoveredGoals() {
        return new HashSet<>(coveredGoals);
    }
}
