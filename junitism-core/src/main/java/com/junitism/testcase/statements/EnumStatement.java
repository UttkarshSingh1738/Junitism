package com.junitism.testcase.statements;

import com.junitism.analysis.TypeInfo;
import com.junitism.execution.ExecutionContext;
import com.junitism.testcase.Scope;
import com.junitism.testcase.Statement;
import com.junitism.testcase.VariableReference;

import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
public class EnumStatement implements Statement {

    private final TypeInfo enumType;
    private final String constantName;
    private final VariableReference returnValue;

    public EnumStatement(TypeInfo enumType, String constantName, VariableReference returnValue) {
        this.enumType = enumType;
        this.constantName = constantName;
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
        return new EnumStatement(enumType, constantName, returnValue);
    }

    @Override
    public void execute(Scope scope) throws ReflectiveOperationException {
        Class<? extends Enum> clazz = (Class<? extends Enum>) ExecutionContext.loadClass(enumType.getBinaryName());
        Enum value = Enum.valueOf(clazz, constantName);
        scope.setVariable(returnValue.getName(), value);
    }
}
