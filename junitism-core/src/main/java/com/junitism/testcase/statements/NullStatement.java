package com.junitism.testcase.statements;

import com.junitism.analysis.TypeInfo;
import com.junitism.testcase.Scope;
import com.junitism.testcase.Statement;
import com.junitism.testcase.VariableReference;

import java.util.Collections;
import java.util.List;

public class NullStatement implements Statement {

    private final VariableReference returnValue;

    public NullStatement(VariableReference returnValue) {
        this.returnValue = returnValue;
    }

    @Override
    public VariableReference getReturnValue() {
        return returnValue;
    }

    @Override
    public List<VariableReference> getInputs() {
        return Collections.emptyList();
    }

    @Override
    public Statement copy() {
        return new NullStatement(returnValue);
    }

    @Override
    public void execute(Scope scope) throws ReflectiveOperationException {
        scope.setVariable(returnValue.getName(), null);
    }
}
