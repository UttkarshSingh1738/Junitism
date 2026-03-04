package com.junitism.testcase;

import com.junitism.analysis.TypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Track live variables by type for a given position in the test case.
 */
public class VariablePool {

    private final List<VariableReference> variables = new ArrayList<>();

    public void add(VariableReference ref) {
        variables.add(ref);
    }

    public List<VariableReference> getVariablesOfType(TypeInfo type, int beforePosition) {
        if (type == null) return List.of();
        return variables.stream()
                .filter(v -> v.getPosition() < beforePosition)
                .filter(v -> isAssignable(v.getType(), type))
                .collect(Collectors.toList());
    }

    private boolean isAssignable(TypeInfo from, TypeInfo to) {
        if (from == null || to == null) return false;
        if (from.equals(to)) return true;
        if (to.getInternalName() != null && "java/lang/Object".equals(to.getInternalName())) return true;
        return false;
    }

    public List<VariableReference> getAll() {
        return new ArrayList<>(variables);
    }

    public void clear() {
        variables.clear();
    }
}
