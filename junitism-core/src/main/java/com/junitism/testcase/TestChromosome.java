package com.junitism.testcase;

import com.junitism.ga.BranchCoverageGoal;
import com.junitism.runtime.BranchExecutionData;

import java.util.HashMap;
import java.util.Map;

/**
 * GA wrapper around TestCase with fitness and execution data.
 */
public class TestChromosome {

    private final TestCase testCase;
    private Map<BranchCoverageGoal, Double> fitnessValues = new HashMap<>();
    private BranchExecutionData lastExecutionData;
    private boolean timedOut;

    public TestChromosome(TestCase testCase) {
        this.testCase = testCase;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public int size() {
        return testCase.size();
    }

    public void setFitnessValues(Map<BranchCoverageGoal, Double> fitnessValues) {
        this.fitnessValues = fitnessValues != null ? new HashMap<>(fitnessValues) : new HashMap<>();
    }

    public Map<BranchCoverageGoal, Double> getFitnessValues() {
        return new HashMap<>(fitnessValues);
    }

    public double getFitness(BranchCoverageGoal goal) {
        return fitnessValues.getOrDefault(goal, Double.MAX_VALUE);
    }

    public void setLastExecutionData(BranchExecutionData data) {
        this.lastExecutionData = data;
    }

    public BranchExecutionData getLastExecutionData() {
        return lastExecutionData;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public TestChromosome copy() {
        TestChromosome copy = new TestChromosome(testCase.copy());
        copy.fitnessValues = new HashMap<>(fitnessValues);
        copy.lastExecutionData = lastExecutionData;
        copy.timedOut = timedOut;
        return copy;
    }
}
