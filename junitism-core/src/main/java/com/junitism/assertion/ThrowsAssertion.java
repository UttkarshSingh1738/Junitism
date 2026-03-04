package com.junitism.assertion;

import com.junitism.testcase.statements.MethodStatement;

public record ThrowsAssertion(MethodStatement statement, Class<? extends Throwable> exceptionType) implements Assertion {

    @Override
    public Assertion copy() {
        return new ThrowsAssertion(statement, exceptionType);
    }
}
