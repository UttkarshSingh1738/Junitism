package com.junitism.assertion;

import com.junitism.testcase.VariableReference;

public record NullAssertion(VariableReference variable) implements Assertion {

    @Override
    public Assertion copy() {
        return new NullAssertion(variable);
    }
}
