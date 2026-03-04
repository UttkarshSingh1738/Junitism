package com.junitism.testcase.statements;

import com.junitism.analysis.ConstructorInfo;
import com.junitism.execution.ExecutionContext;
import com.junitism.analysis.TypeInfo;
import com.junitism.testcase.Scope;
import com.junitism.testcase.Statement;
import com.junitism.testcase.VariableReference;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class ConstructorStatement implements Statement {

    private final ConstructorInfo constructor;
    private final List<VariableReference> args;
    private final VariableReference returnValue;

    public ConstructorStatement(ConstructorInfo constructor, List<VariableReference> args, VariableReference returnValue) {
        this.constructor = constructor;
        this.args = args != null ? new ArrayList<>(args) : new ArrayList<>();
        this.returnValue = returnValue;
    }

    @Override
    public VariableReference getReturnValue() {
        return returnValue;
    }

    @Override
    public List<VariableReference> getInputs() {
        return new ArrayList<>(args);
    }

    @Override
    public Statement copy() {
        return new ConstructorStatement(constructor, args, returnValue);
    }

    @Override
    public void execute(Scope scope) throws ReflectiveOperationException {
        Class<?> clazz = ExecutionContext.loadClass(constructor.getDeclaringClass().getBinaryName());
        Class<?>[] paramTypes = new Class<?>[args.size()];
        for (int i = 0; i < args.size(); i++) {
            paramTypes[i] = toClass(args.get(i).getType());
        }
        Constructor<?> ctor = clazz.getDeclaredConstructor(paramTypes);
        ctor.setAccessible(true);
        Object[] argValues = args.stream()
                .map(a -> scope.getVariable(a))
                .toArray();
        Object instance = ctor.newInstance(argValues);
        scope.setVariable(returnValue.getName(), instance);
    }

    private static Class<?> toClass(TypeInfo t) throws ReflectiveOperationException {
        if (t.isPrimitive()) {
            return switch (t.getInternalName()) {
                case "int" -> int.class;
                case "long" -> long.class;
                case "double" -> double.class;
                case "float" -> float.class;
                case "boolean" -> boolean.class;
                case "byte" -> byte.class;
                case "short" -> short.class;
                case "char" -> char.class;
                default -> ExecutionContext.loadClass(t.getBinaryName());
            };
        }
        return Class.forName(t.getBinaryName());
    }

    public ConstructorInfo getConstructor() {
        return constructor;
    }
}
