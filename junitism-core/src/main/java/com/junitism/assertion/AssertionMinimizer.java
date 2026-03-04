package com.junitism.assertion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Removes redundant and unstable assertions.
 */
public class AssertionMinimizer {

    public List<Assertion> minimize(List<Assertion> assertions) {
        List<Assertion> result = new ArrayList<>(assertions);
        result = removeRedundant(result);
        return result;
    }

    private List<Assertion> removeRedundant(List<Assertion> assertions) {
        Set<Assertion> toRemove = new HashSet<>();
        for (Assertion a : assertions) {
            if (a instanceof NotNullAssertion) {
                for (Assertion b : assertions) {
                    if (a != b && b instanceof EqualsAssertion ea) {
                        if (ea.variable().equals(((NotNullAssertion) a).variable())) {
                            toRemove.add(a);
                            break;
                        }
                    }
                }
            }
        }
        List<Assertion> result = new ArrayList<>();
        for (Assertion a : assertions) {
            if (!toRemove.contains(a)) result.add(a);
        }
        return result;
    }
}
