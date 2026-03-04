package com.junitism.assertion;

import java.lang.reflect.Field;

public record FieldAssertion(Field field, Object expectedValue) implements Assertion {

    @Override
    public Assertion copy() {
        return new FieldAssertion(field, expectedValue);
    }
}
