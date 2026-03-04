package com.junitism.testcase;

import com.junitism.analysis.TypeInfo;

import java.util.Objects;

/**
 * Typed reference to a value in the test case.
 */
public class VariableReference {

    private final int position;
    private final TypeInfo type;
    private final String name;

    public VariableReference(int position, TypeInfo type, String name) {
        this.position = position;
        this.type = type;
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    public TypeInfo getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableReference that = (VariableReference) o;
        return position == that.position && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, type);
    }
}
