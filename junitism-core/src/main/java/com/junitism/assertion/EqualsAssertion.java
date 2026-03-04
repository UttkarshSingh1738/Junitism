package com.junitism.assertion;

import com.junitism.testcase.VariableReference;

public record EqualsAssertion(VariableReference variable, Object expectedValue) implements Assertion {

    @Override
    public Assertion copy() {
        return new EqualsAssertion(variable, expectedValue);
    }
}
