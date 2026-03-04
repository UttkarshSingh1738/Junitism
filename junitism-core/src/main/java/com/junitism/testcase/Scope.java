package com.junitism.testcase;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution scope: variable name to value mapping.
 */
public class Scope {

    private final Map<String, Object> variables = new HashMap<>();

    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    public Object getVariable(String name) {
        return variables.get(name);
    }

    public Object getVariable(VariableReference ref) {
        return ref != null ? variables.get(ref.getName()) : null;
    }

    public Map<String, Object> getVariables() {
        return new HashMap<>(variables);
    }

    public void clear() {
        variables.clear();
    }
}
