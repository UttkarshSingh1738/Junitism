package com.junitism.ga;

import com.junitism.testcase.TestChromosome;

import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Best test per active goal for elitism.
 */
public class PreferenceCriterion {

    public Set<TestChromosome> getPreferred(List<TestChromosome> population,
                                            Set<BranchCoverageGoal> activeGoals) {
        Set<TestChromosome> preferred = new HashSet<>();
        for (BranchCoverageGoal goal : activeGoals) {
            Optional<TestChromosome> best = population.stream()
                    .min(Comparator.comparingDouble((TestChromosome t) -> t.getFitness(goal)));
            best.ifPresent(preferred::add);
        }
        return preferred;
    }
}
