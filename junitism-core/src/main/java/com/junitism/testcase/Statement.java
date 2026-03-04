package com.junitism.testcase;

import com.junitism.analysis.TypeInfo;

import java.util.List;

/**
 * Base interface for test case statements.
 */
public interface Statement {

    VariableReference getReturnValue();

    List<VariableReference> getInputs();

    Statement copy();

    void execute(Scope scope) throws ReflectiveOperationException;
}
