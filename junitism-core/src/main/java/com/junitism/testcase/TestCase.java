package com.junitism.testcase;

import com.junitism.analysis.TypeInfo;
import com.junitism.testcase.statements.MethodStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ordered sequence of statements forming a test case.
 */
public class TestCase {

    private final List<Statement> statements = new ArrayList<>();
    private final VariablePool variablePool = new VariablePool();

    public void addStatement(Statement stmt) {
        int pos = statements.size();
        statements.add(stmt);
        if (stmt.getReturnValue() != null) {
            variablePool.add(stmt.getReturnValue());
        }
    }

    public void insertStatement(int index, Statement stmt) {
        statements.add(index, stmt);
        if (stmt.getReturnValue() != null) {
            variablePool.add(stmt.getReturnValue());
        }
    }

    public void removeStatement(int index) {
        statements.remove(index);
    }

    public List<Statement> getMutableStatements() {
        return statements;
    }

    public List<Statement> getStatements() {
        return new ArrayList<>(statements);
    }

    public VariablePool getVariablePool() {
        return variablePool;
    }

    public int size() {
        return statements.size();
    }

    public Statement getStatement(int index) {
        return statements.get(index);
    }

    public List<MethodStatement> getMethodStatements() {
        return statements.stream()
                .filter(s -> s instanceof MethodStatement)
                .map(s -> (MethodStatement) s)
                .collect(Collectors.toList());
    }

    public TestCase copy() {
        TestCase copy = new TestCase();
        for (Statement s : statements) {
            copy.addStatement(s.copy());
        }
        return copy;
    }

    public List<VariableReference> getVariablesOfType(TypeInfo type, int beforePosition) {
        List<VariableReference> result = new ArrayList<>();
        for (int i = 0; i < Math.min(beforePosition, statements.size()); i++) {
            Statement s = statements.get(i);
            VariableReference r = s.getReturnValue();
            if (r != null && isAssignable(r.getType(), type)) {
                result.add(r);
            }
        }
        return result;
    }

    private boolean isAssignable(TypeInfo from, TypeInfo to) {
        if (from == null || to == null) return false;
        if (from.equals(to)) return true;
        if (to.getInternalName() != null && "java/lang/Object".equals(to.getInternalName())) return true;
        return false;
    }
}
