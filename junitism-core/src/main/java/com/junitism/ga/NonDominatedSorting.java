package com.junitism.ga;

import com.junitism.testcase.TestChromosome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NSGA-II fast non-dominated sort.
 */
public class NonDominatedSorting {

    public List<List<TestChromosome>> sort(List<TestChromosome> population,
                                          Set<BranchCoverageGoal> activeGoals) {
        List<TestChromosome> list = new ArrayList<>(population);
        List<List<TestChromosome>> fronts = new ArrayList<>();

        while (!list.isEmpty()) {
            List<TestChromosome> front = new ArrayList<>();
            for (TestChromosome p : list) {
                int dominatedCount = 0;
                for (TestChromosome q : list) {
                    if (p != q && dominates(q, p, activeGoals)) {
                        dominatedCount++;
                    }
                }
                if (dominatedCount == 0) {
                    front.add(p);
                }
            }
            list.removeAll(front);
            fronts.add(front);
        }
        return fronts;
    }

    private boolean dominates(TestChromosome a, TestChromosome b, Set<BranchCoverageGoal> goals) {
        boolean betterOnAny = false;
        for (BranchCoverageGoal g : goals) {
            double fa = a.getFitness(g);
            double fb = b.getFitness(g);
            if (fa > fb) return false;
            if (fa < fb) betterOnAny = true;
        }
        return betterOnAny;
    }
}
