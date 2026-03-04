package com.junitism.ga;

import com.junitism.instrumentation.ControlDependencyGraph;
import com.junitism.runtime.BranchExecutionData;
import com.junitism.testcase.TestChromosome;

import java.util.Set;

/**
 * Fitness = approach_level + normalize(branch_distance).
 */
public class FitnessFunction {

    private final ControlDependencyGraph cdg;

    public FitnessFunction(ControlDependencyGraph cdg) {
        this.cdg = cdg;
    }

    public double evaluate(TestChromosome test, BranchCoverageGoal goal) {
        BranchExecutionData data = test.getLastExecutionData();
        if (data == null) return Double.MAX_VALUE;

        if (data.wasBranchExecuted(goal.getBranchId())) {
            double branchDist = data.getBranchDistance(goal.getBranchId(), goal.isTrue());
            return normalize(branchDist);
        } else {
            int approachLevel = computeApproachLevel(data, goal);
            return approachLevel + 1.0;
        }
    }

    private int computeApproachLevel(BranchExecutionData data, BranchCoverageGoal goal) {
        Set<Integer> deps = cdg.getDependencies(goal.getBranchId());
        int count = 0;
        for (Integer depId : deps) {
            if (!data.wasBranchExecuted(depId)) {
                count++;
            }
        }
        return count;
    }

    public static double normalize(double distance) {
        return distance / (distance + 1.0);
    }
}
