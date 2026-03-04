package com.junitism.assertion;

import com.junitism.testcase.VariableReference;

public record NotNullAssertion(VariableReference variable) implements Assertion {

    @Override
    public Assertion copy() {
        return new NotNullAssertion(variable);
    }
}
