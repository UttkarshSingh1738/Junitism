package com.junitism.testcase.statements;

import com.junitism.analysis.TypeInfo;
import com.junitism.execution.ExecutionContext;
import com.junitism.testcase.Scope;
import com.junitism.testcase.Statement;
import com.junitism.testcase.VariableReference;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;

public class ArrayStatement implements Statement {

    private final TypeInfo componentType;
    private final int length;
    private final VariableReference returnValue;
    private final VariableReference lengthVar;

    public ArrayStatement(TypeInfo componentType, int length, VariableReference returnValue) {
        this.componentType = componentType;
        this.length = length;
        this.returnValue = returnValue;
        this.lengthVar = null;
    }

    public ArrayStatement(TypeInfo componentType, VariableReference lengthVar, VariableReference returnValue) {
        this.componentType = componentType;
        this.length = -1;
        this.returnValue = returnValue;
        this.lengthVar = lengthVar;
    }

    @Override
    public VariableReference getReturnValue() {
        return returnValue;
    }

    @Override
    public List<VariableReference> getInputs() {
        return lengthVar != null ? List.of(lengthVar) : Collections.emptyList();
    }

    @Override
    public Statement copy() {
        return lengthVar != null
                ? new ArrayStatement(componentType, lengthVar, returnValue)
                : new ArrayStatement(componentType, length, returnValue);
    }

    @Override
    public void execute(Scope scope) throws ReflectiveOperationException {
        int len = lengthVar != null ? ((Number) scope.getVariable(lengthVar)).intValue() : Math.max(0, length);
        Class<?> compClass = componentType != null && componentType.isPrimitive()
                ? toPrimitiveClass(componentType.getInternalName())
                : (componentType != null ? ExecutionContext.loadClass(componentType.getBinaryName()) : Object.class);
        Object array = Array.newInstance(compClass, len);
        scope.setVariable(returnValue.getName(), array);
    }

    private static Class<?> toPrimitiveClass(String name) {
        return switch (name) {
            case "int" -> int.class;
            case "long" -> long.class;
            case "double" -> double.class;
            case "float" -> float.class;
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "char" -> char.class;
            default -> Object.class;
        };
    }
}
