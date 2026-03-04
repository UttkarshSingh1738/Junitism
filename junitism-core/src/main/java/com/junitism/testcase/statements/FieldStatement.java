package com.junitism.testcase.statements;

import com.junitism.analysis.FieldInfo;
import com.junitism.execution.ExecutionContext;
import com.junitism.testcase.Scope;
import com.junitism.testcase.Statement;
import com.junitism.testcase.VariableReference;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class FieldStatement implements Statement {

    private final VariableReference target;
    private final FieldInfo field;
    private final VariableReference returnValue;
    private final boolean isRead;

    public FieldStatement(VariableReference target, FieldInfo field, VariableReference returnValue, boolean isRead) {
        this.target = target;
        this.field = field;
        this.returnValue = returnValue;
        this.isRead = isRead;
    }

    @Override
    public VariableReference getReturnValue() {
        return returnValue;
    }

    @Override
    public List<VariableReference> getInputs() {
        return target != null ? List.of(target) : Collections.emptyList();
    }

    @Override
    public Statement copy() {
        return new FieldStatement(target, field, returnValue, isRead);
    }

    @Override
    public void execute(Scope scope) throws ReflectiveOperationException {
        Class<?> clazz = ExecutionContext.loadClass(field.getDeclaringClass().getBinaryName());
        Field f = clazz.getDeclaredField(field.getName());
        f.setAccessible(true);
        Object obj = target != null ? scope.getVariable(target) : null;
        if (isRead && returnValue != null) {
            Object value = f.get(obj);
            scope.setVariable(returnValue.getName(), value);
        } else if (!isRead && target != null) {
            f.set(obj, scope.getVariable(returnValue));
        }
    }
}
