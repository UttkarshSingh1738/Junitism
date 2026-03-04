package com.junitism.assertion;

import com.junitism.execution.ExecutionResult;
import com.junitism.execution.TestExecutor;
import com.junitism.testcase.TestCase;
import com.junitism.testcase.TestChromosome;
import com.junitism.testcase.statements.MethodStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates assertions from observed execution state.
 */
public class AssertionGenerator {

    private final TestExecutor executor;

    public AssertionGenerator(TestExecutor executor) {
        this.executor = executor;
    }

    public List<Assertion> generate(TestCase test) {
        TestChromosome chromosome = new TestChromosome(test);
        ExecutionResult result = executor.execute(chromosome);
        List<Assertion> assertions = new ArrayList<>();

        if (!result.isSuccess() || result.getReturnValues() == null) {
            return assertions;
        }
        for (MethodStatement stmt : test.getMethodStatements()) {
            if (stmt.getReturnValue() != null) {
                Object returnValue = result.getReturnValue(stmt.getReturnValue().getName());
                if (returnValue != null) {
                    if (isPrimitiveOrString(returnValue)) {
                        assertions.add(new EqualsAssertion(stmt.getReturnValue(), returnValue));
                    } else {
                        assertions.add(new NotNullAssertion(stmt.getReturnValue()));
                    }
                } else if (stmt.getReturnType() != null && !stmt.getReturnType().isVoid()) {
                    assertions.add(new NullAssertion(stmt.getReturnValue()));
                }
            }
        }
        if (result.getException() != null && !test.getMethodStatements().isEmpty()) {
            assertions.add(new ThrowsAssertion(test.getMethodStatements().get(test.getMethodStatements().size() - 1),
                    result.getException().getClass()));
        }
        return assertions;
    }

    private boolean isPrimitiveOrString(Object value) {
        return value instanceof Number || value instanceof Boolean || value instanceof Character
                || value instanceof String;
    }
}
