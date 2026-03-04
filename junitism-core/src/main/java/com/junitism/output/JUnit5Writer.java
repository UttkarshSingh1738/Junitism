package com.junitism.output;

import com.junitism.assertion.Assertion;
import com.junitism.assertion.EqualsAssertion;
import com.junitism.assertion.NotNullAssertion;
import com.junitism.assertion.NullAssertion;
import com.junitism.assertion.ThrowsAssertion;
import com.junitism.testcase.TestCase;
import com.junitism.testcase.statements.ConstructorStatement;
import com.junitism.testcase.statements.MethodStatement;
import com.junitism.testcase.statements.PrimitiveStatement;

import java.util.List;

/**
 * Produces JUnit 5 .java test files.
 */
public class JUnit5Writer {

    private final TestFormatter formatter = new TestFormatter();

    public String write(String targetClassName, List<TestCase> testSuite, List<List<Assertion>> assertionsPerTest) {
        StringBuilder sb = new StringBuilder();
        String pkg = formatter.deriveTestPackage(targetClassName);
        if (!pkg.isEmpty()) {
            sb.append("package ").append(pkg).append(";\n\n");
        }
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n");
        sb.append("import ").append(targetClassName).append(";\n\n");

        sb.append("class ").append(formatter.simpleTestClassName(targetClassName)).append(" {\n\n");

        for (int i = 0; i < testSuite.size(); i++) {
            TestCase tc = testSuite.get(i);
            List<Assertion> assertions = i < assertionsPerTest.size() ? assertionsPerTest.get(i) : List.of();
            sb.append(createTestMethod(tc, "test" + i, assertions));
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String createTestMethod(TestCase tc, String methodName, List<Assertion> assertions) {
        StringBuilder sb = new StringBuilder();
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"").append(methodName).append("\")\n");
        sb.append("    void ").append(methodName).append("() {\n");

        for (var stmt : tc.getStatements()) {
            sb.append("        ").append(statementToJava(stmt)).append("\n");
        }
        for (Assertion a : assertions) {
            sb.append("        ").append(assertionToJava(a)).append("\n");
        }
        sb.append("    }\n\n");
        return sb.toString();
    }

    private String statementToJava(com.junitism.testcase.Statement stmt) {
        if (stmt instanceof ConstructorStatement cs) {
            String typeName = cs.getConstructor().getDeclaringClass().getBinaryName();
            return typeName + " " + cs.getReturnValue().getName() + " = new " + typeName + "();";
        }
        if (stmt instanceof MethodStatement ms) {
            String target = ms.getTarget() != null ? ms.getTarget().getName() + "." : "";
            StringBuilder args = new StringBuilder();
            for (var arg : ms.getInputs()) {
                if (arg != ms.getTarget()) {
                    if (args.length() > 0) args.append(", ");
                    args.append(arg.getName());
                }
            }
            String call = target + ms.getMethod().getName() + "(" + args + ")";
            if (ms.getReturnValue() != null && ms.getReturnType() != null && !ms.getReturnType().isVoid()) {
                return ms.getReturnType().getBinaryName() + " " + ms.getReturnValue().getName() + " = " + call + ";";
            }
            return call + ";";
        }
        if (stmt instanceof PrimitiveStatement ps) {
            Object v = ps.getValue();
            String typeStr = ps.getReturnValue().getType() != null ? ps.getReturnValue().getType().getBinaryName() : "Object";
            return typeStr + " " + ps.getReturnValue().getName() + " = " + literalToJava(v) + ";";
        }
        return "";
    }

    private String literalToJava(Object v) {
        if (v == null) return "null";
        if (v instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        if (v instanceof Integer) return v.toString();
        if (v instanceof Long) return v + "L";
        if (v instanceof Double) return v.toString();
        if (v instanceof Float) return v + "f";
        if (v instanceof Boolean) return v.toString();
        return String.valueOf(v);
    }

    private String assertionToJava(Assertion a) {
        if (a instanceof EqualsAssertion ea) {
            return "assertEquals(" + literalToJava(ea.expectedValue()) + ", " + ea.variable().getName() + ");";
        }
        if (a instanceof NotNullAssertion na) {
            return "assertNotNull(" + na.variable().getName() + ");";
        }
        if (a instanceof NullAssertion na) {
            return "assertNull(" + na.variable().getName() + ");";
        }
        return "";
    }
}
