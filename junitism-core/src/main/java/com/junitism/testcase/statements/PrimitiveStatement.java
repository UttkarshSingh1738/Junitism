package com.junitism.testcase.statements;

import com.junitism.analysis.TypeInfo;
import com.junitism.testcase.Scope;
import com.junitism.testcase.Statement;
import com.junitism.testcase.VariableReference;

import java.util.Collections;
import java.util.List;

public class PrimitiveStatement implements Statement {

    private final Object value;
    private final VariableReference returnValue;

    public PrimitiveStatement(Object value, VariableReference returnValue) {
        this.value = value;
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
        return new PrimitiveStatement(value, returnValue);
    }

    @Override
    public void execute(Scope scope) throws ReflectiveOperationException {
        scope.setVariable(returnValue.getName(), value);
    }

    public Object getValue() {
        return value;
    }
}
