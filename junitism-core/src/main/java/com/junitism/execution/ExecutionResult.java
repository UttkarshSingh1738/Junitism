package com.junitism.execution;

import com.junitism.runtime.BranchExecutionData;

import java.util.Map;

/**
 * Result of test execution: success, exception, branch data, return values.
 */
public class ExecutionResult {

    private final boolean success;
    private final Throwable exception;
    private final BranchExecutionData branchData;
    private final Map<String, Object> returnValues;
    private final boolean timedOut;

    public ExecutionResult(boolean success, Throwable exception, BranchExecutionData branchData,
                           Map<String, Object> returnValues) {
        this(success, exception, branchData, returnValues, false);
    }

    public ExecutionResult(boolean success, Throwable exception, BranchExecutionData branchData,
                           Map<String, Object> returnValues, boolean timedOut) {
        this.success = success;
        this.exception = exception;
        this.branchData = branchData;
        this.returnValues = returnValues;
        this.timedOut = timedOut;
    }

    public static ExecutionResult timeout(BranchExecutionData branchData) {
        return new ExecutionResult(false, null, branchData, null, true);
    }

    public boolean isSuccess() {
        return success;
    }

    public Throwable getException() {
        return exception;
    }

    public BranchExecutionData getBranchData() {
        return branchData;
    }

    public Map<String, Object> getReturnValues() {
        return returnValues;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public Object getReturnValue(String variableName) {
        return returnValues != null ? returnValues.get(variableName) : null;
    }
}
