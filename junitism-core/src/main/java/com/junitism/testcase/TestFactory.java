package com.junitism.testcase;

import com.junitism.analysis.ConstructorInfo;
import com.junitism.analysis.MethodInfo;
import com.junitism.analysis.TestCluster;
import com.junitism.analysis.TypeInfo;
import com.junitism.testcase.statements.ConstructorStatement;
import com.junitism.testcase.statements.MethodStatement;
import com.junitism.testcase.statements.PrimitiveStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates random test cases for population initialization.
 */
public class TestFactory {

    private final TestCluster cluster;
    private final Random random;
    private int maxStatements = 80;
    private int maxRecursionDepth = 5;

    public TestFactory(TestCluster cluster) {
        this(cluster, new Random());
    }

    public TestFactory(TestCluster cluster, Random random) {
        this.cluster = cluster;
        this.random = random;
    }

    public TestFactory(TestCluster cluster, long seed) {
        this.cluster = cluster;
        this.random = new Random(seed);
    }

    public void setMaxStatements(int max) {
        this.maxStatements = max;
    }

    public TestCase generateRandom() {
        TestCase tc = new TestCase();
        int targetLength = Math.max(1, random.nextInt(maxStatements) + 1);

        while (tc.size() < targetLength) {
            MethodInfo method = cluster.getRandomTargetMethod();
            if (method == null) break;

            ensureInstanceExists(tc, method.getDeclaringClass(), 0);
            List<VariableReference> targetVars = tc.getVariablesOfType(method.getDeclaringClass(), tc.size());
            if (targetVars.isEmpty()) continue;

            List<VariableReference> args = new ArrayList<>();
            for (TypeInfo paramType : method.getParameterTypes()) {
                ensureInstanceExists(tc, paramType, 0);
                List<VariableReference> candidates = tc.getVariablesOfType(paramType, tc.size());
                if (!candidates.isEmpty()) {
                    args.add(candidates.get(random.nextInt(candidates.size())));
                }
            }

            if (args.size() == method.getParameterTypes().size() || method.getParameterTypes().isEmpty()) {
                VariableReference ret = method.getReturnType() != null && !method.getReturnType().isVoid()
                        ? new VariableReference(tc.size(), method.getReturnType(), "var" + tc.size())
                        : null;
                tc.addStatement(new MethodStatement(targetVars.get(0), method, args, ret));
            }
        }
        return tc;
    }

    private void ensureInstanceExists(TestCase tc, TypeInfo type, int depth) {
        if (depth > maxRecursionDepth) return;
        if (type == null || type.isVoid()) return;

        List<VariableReference> existing = tc.getVariablesOfType(type, tc.size());
        if (!existing.isEmpty()) return;

        if (type.isPrimitive()) {
            Object value = randomPrimitive(type);
            VariableReference ret = new VariableReference(tc.size(), type, "var" + tc.size());
            tc.addStatement(new PrimitiveStatement(value, ret));
            return;
        }

        if ("java.lang.String".equals(type.getBinaryName())) {
            String value = random.nextBoolean() ? "" : "s" + random.nextInt(1000);
            VariableReference ret = new VariableReference(tc.size(), type, "var" + tc.size());
            tc.addStatement(new PrimitiveStatement(value, ret));
            return;
        }

        List<ConstructorInfo> ctors = cluster.getConstructors(type);
        if (!ctors.isEmpty()) {
            ConstructorInfo ctor = ctors.get(random.nextInt(ctors.size()));
            List<VariableReference> args = new ArrayList<>();
            for (TypeInfo paramType : ctor.getParameterTypes()) {
                ensureInstanceExists(tc, paramType, depth + 1);
                List<VariableReference> candidates = tc.getVariablesOfType(paramType, tc.size());
                if (!candidates.isEmpty()) {
                    args.add(candidates.get(random.nextInt(candidates.size())));
                }
            }
            if (args.size() == ctor.getParameterTypes().size()) {
                VariableReference ret = new VariableReference(tc.size(), type, "var" + tc.size());
                tc.addStatement(new ConstructorStatement(ctor, args, ret));
            }
        }
    }

    private Object randomPrimitive(TypeInfo type) {
        return switch (type.getInternalName()) {
            case "int" -> random.nextInt(100) - 50;
            case "long" -> random.nextLong() % 100;
            case "double" -> random.nextDouble() * 100;
            case "float" -> random.nextFloat() * 100;
            case "boolean" -> random.nextBoolean();
            case "byte" -> (byte) random.nextInt(256);
            case "short" -> (short) random.nextInt(1000);
            case "char" -> (char) ('a' + random.nextInt(26));
            default -> 0;
        };
    }
}
