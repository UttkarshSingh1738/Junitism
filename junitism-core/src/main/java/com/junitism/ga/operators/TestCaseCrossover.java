package com.junitism.ga.operators;

import com.junitism.analysis.TypeInfo;
import com.junitism.testcase.Statement;
import com.junitism.testcase.TestCase;
import com.junitism.testcase.VariableReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Single-point crossover for test cases.
 */
public class TestCaseCrossover {

    private final Random random;
    private double crossoverRate = 0.75;

    public TestCaseCrossover(Random random) {
        this.random = random;
    }

    public TestCaseCrossover(long seed) {
        this.random = new Random(seed);
    }

    public void setCrossoverRate(double rate) {
        this.crossoverRate = rate;
    }

    public record Pair(TestCase first, TestCase second) {}

    public Pair crossover(TestCase parent1, TestCase parent2) {
        if (parent1.size() < 2 || parent2.size() < 2) {
            return new Pair(parent1.copy(), parent2.copy());
        }

        int alpha = 1 + random.nextInt(parent1.size() - 1);
        int beta = 1 + random.nextInt(parent2.size() - 1);

        TestCase offspring1 = merge(parent1, 0, alpha, parent2, beta, parent2.size());
        TestCase offspring2 = merge(parent2, 0, beta, parent1, alpha, parent1.size());

        return new Pair(offspring1, offspring2);
    }

    private TestCase merge(TestCase prefixSource, int prefixStart, int prefixEnd,
                          TestCase suffixSource, int suffixStart, int suffixEnd) {
        TestCase result = new TestCase();
        for (int i = prefixStart; i < prefixEnd; i++) {
            result.addStatement(prefixSource.getStatement(i).copy());
        }
        for (int i = suffixStart; i < suffixEnd; i++) {
            Statement stmt = suffixSource.getStatement(i).copy();
            VariableReference ret = stmt.getReturnValue();
            if (ret != null) {
                List<VariableReference> candidates = result.getVariablesOfType(ret.getType(), result.size());
                if (candidates.isEmpty() && !stmt.getInputs().isEmpty()) {
                    continue;
                }
            }
            result.addStatement(stmt);
        }
        return result;
    }

    public boolean shouldCrossover() {
        return random.nextDouble() < crossoverRate;
    }
}
