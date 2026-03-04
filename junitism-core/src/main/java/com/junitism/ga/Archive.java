package com.junitism.ga;

import com.junitism.testcase.TestCase;
import com.junitism.testcase.TestChromosome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Best test per coverage goal. Updates on improvement.
 */
public class Archive {

    private final Map<BranchCoverageGoal, TestChromosome> bestTests = new HashMap<>();

    public void update(TestChromosome test, Map<BranchCoverageGoal, Double> fitnessValues) {
        for (Map.Entry<BranchCoverageGoal, Double> entry : fitnessValues.entrySet()) {
            BranchCoverageGoal goal = entry.getKey();
            double fitness = entry.getValue();

            if (fitness == 0.0) {
                TestChromosome current = bestTests.get(goal);
                if (current == null || test.size() < current.size()) {
                    bestTests.put(goal, test.copy());
                }
            }
        }
    }

    public Set<BranchCoverageGoal> getCoveredGoals() {
        return new HashSet<>(bestTests.keySet());
    }

    public List<TestCase> getFinalTestSuite() {
        Set<TestCase> unique = new HashSet<>();
        for (TestChromosome tc : bestTests.values()) {
            unique.add(tc.getTestCase());
        }
        return new ArrayList<>(unique);
    }

    public TestChromosome getBestFor(BranchCoverageGoal goal) {
        return bestTests.get(goal);
    }

    public int size() {
        return bestTests.size();
    }
}
