package com.junitism.ga.operators;

import com.junitism.analysis.ConstructorInfo;
import com.junitism.analysis.MethodInfo;
import com.junitism.analysis.TestCluster;
import com.junitism.analysis.TypeInfo;
import com.junitism.testcase.Statement;
import com.junitism.testcase.TestCase;
import com.junitism.testcase.VariableReference;
import com.junitism.testcase.statements.ConstructorStatement;
import com.junitism.testcase.statements.MethodStatement;
import com.junitism.testcase.statements.PrimitiveStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mutation operators: INSERT, DELETE, CHANGE, REPLACE.
 */
public class TestCaseMutation {

    private final Random random;
    private final TestCluster cluster;
    private double mutationRate = 0.1;

    public TestCaseMutation(Random random, TestCluster cluster) {
        this.random = random;
        this.cluster = cluster;
    }

    public TestCaseMutation(long seed, TestCluster cluster) {
        this.random = new Random(seed);
        this.cluster = cluster;
    }

    public void setMutationRate(double rate) {
        this.mutationRate = rate;
    }

    public TestCase mutate(TestCase original) {
        TestCase mutated = original.copy();
        int len = mutated.size();
        if (len == 0) return mutated;

        for (int i = 0; i < len; i++) {
            if (random.nextDouble() < mutationRate) {
                switch (random.nextInt(4)) {
                    case 0 -> insertRandomStatement(mutated, i);
                    case 1 -> deleteStatement(mutated, i);
                    case 2 -> changeStatement(mutated, i);
                    case 3 -> replaceStatement(mutated, i);
                }
                len = mutated.size();
            }
        }
        return mutated;
    }

    private void insertRandomStatement(TestCase tc, int position) {
        MethodInfo method = cluster.getRandomTargetMethod();
        if (method == null) return;
        TypeInfo targetType = method.getDeclaringClass();
        List<VariableReference> targetVars = tc.getVariablesOfType(targetType, position);
        if (targetVars.isEmpty()) {
            List<ConstructorInfo> ctors = cluster.getConstructors(targetType);
            if (!ctors.isEmpty()) {
                ConstructorInfo ctor = ctors.get(random.nextInt(ctors.size()));
                List<VariableReference> args = resolveArgs(tc, position, ctor.getParameterTypes());
                VariableReference ret = new VariableReference(position, targetType, "var" + position);
                tc.insertStatement(position, new ConstructorStatement(ctor, args, ret));
            }
        } else {
            List<VariableReference> args = resolveArgs(tc, position, method.getParameterTypes());
            VariableReference ret = new VariableReference(position, method.getReturnType(), "var" + position);
            tc.insertStatement(position, new MethodStatement(targetVars.get(0), method, args, ret));
        }
    }

    private void deleteStatement(TestCase tc, int index) {
        if (tc.size() <= 1) return;
        tc.removeStatement(index);
    }

    private void changeStatement(TestCase tc, int index) {
        Statement stmt = tc.getStatement(index);
        if (stmt instanceof PrimitiveStatement ps) {
            Object value = ps.getValue();
            if (value instanceof Number n) {
                double delta = random.nextDouble() * 10 - 5;
                Object newVal = value instanceof Integer ? (int) (n.intValue() + delta)
                        : value instanceof Long ? (long) (n.longValue() + delta)
                        : value instanceof Double ? n.doubleValue() + delta
                        : value;
                tc.getMutableStatements().set(index, new PrimitiveStatement(newVal, ps.getReturnValue()));
            } else if (value instanceof Boolean) {
                tc.getMutableStatements().set(index, new PrimitiveStatement(!(Boolean) value, ps.getReturnValue()));
            }
        }
    }

    private void replaceStatement(TestCase tc, int index) {
        Statement stmt = tc.getStatement(index);
        if (stmt instanceof MethodStatement ms) {
            List<MethodInfo> methods = cluster.getTargetMethods();
            if (methods.size() > 1) {
                MethodInfo other = methods.get(random.nextInt(methods.size()));
                if (other.getDeclaringClass().equals(ms.getMethod().getDeclaringClass())) {
                    List<VariableReference> args = resolveArgs(tc, index, other.getParameterTypes());
                    tc.getMutableStatements().set(index, new MethodStatement(ms.getTarget(), other, args, ms.getReturnValue()));
                }
            }
        }
    }

    private List<VariableReference> resolveArgs(TestCase tc, int position, List<TypeInfo> paramTypes) {
        List<VariableReference> args = new ArrayList<>();
        for (TypeInfo t : paramTypes) {
            List<VariableReference> candidates = tc.getVariablesOfType(t, position);
            if (!candidates.isEmpty()) {
                args.add(candidates.get(random.nextInt(candidates.size())));
            }
        }
        return args;
    }
}
