package com.junitism.testcase.statements;

import com.junitism.analysis.MethodInfo;
import com.junitism.execution.ExecutionContext;
import com.junitism.analysis.TypeInfo;
import com.junitism.testcase.Scope;
import com.junitism.testcase.Statement;
import com.junitism.testcase.VariableReference;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MethodStatement implements Statement {

    private final VariableReference target;
    private final MethodInfo method;
    private final List<VariableReference> args;
    private final VariableReference returnValue;

    public MethodStatement(VariableReference target, MethodInfo method, List<VariableReference> args, VariableReference returnValue) {
        this.target = target;
        this.method = method;
        this.args = args != null ? new ArrayList<>(args) : new ArrayList<>();
        this.returnValue = returnValue;
    }

    @Override
    public VariableReference getReturnValue() {
        return returnValue;
    }

    @Override
    public List<VariableReference> getInputs() {
        List<VariableReference> inputs = new ArrayList<>();
        if (target != null && !method.isStatic()) inputs.add(target);
        inputs.addAll(args);
        return inputs;
    }

    @Override
    public Statement copy() {
        return new MethodStatement(target, method, args, returnValue);
    }

    @Override
    public void execute(Scope scope) throws ReflectiveOperationException {
        Class<?> clazz = ExecutionContext.loadClass(method.getDeclaringClass().getBinaryName());
        Class<?>[] paramTypes = method.getParameterTypes().stream()
                .map(t -> {
                    try {
                        return toClass(t);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(Class[]::new);
        Method m = clazz.getDeclaredMethod(method.getName(), paramTypes);
        m.setAccessible(true);
        Object obj = method.isStatic() ? null : scope.getVariable(target);
        Object[] argValues = args.stream().map(a -> scope.getVariable(a)).toArray();
        Object result = m.invoke(obj, argValues);
        if (returnValue != null) {
            scope.setVariable(returnValue.getName(), result);
        }
    }

    private static Class<?> toClass(TypeInfo t) throws ClassNotFoundException {
        if (t != null && t.isPrimitive()) {
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
        return t != null ? ExecutionContext.loadClass(t.getBinaryName()) : void.class;
    }

    public VariableReference getTarget() {
        return target;
    }

    public MethodInfo getMethod() {
        return method;
    }

    public TypeInfo getReturnType() {
        return method.getReturnType();
    }
}
