package com.junitism.execution;

import com.junitism.instrumentation.InstrumentingClassLoader;
import com.junitism.runtime.BranchTracker;
import com.junitism.testcase.Scope;
import com.junitism.testcase.Statement;
import com.junitism.testcase.TestChromosome;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Thread-based test execution with timeout.
 */
public class TestExecutor {

    private long defaultTimeoutMs = 5000;
    private InstrumentingClassLoader instrumentingLoader;

    public void setDefaultTimeoutMs(long ms) {
        this.defaultTimeoutMs = ms;
    }

    public void setInstrumentingClassLoader(InstrumentingClassLoader loader) {
        this.instrumentingLoader = loader;
    }

    public ExecutionResult execute(TestChromosome test) {
        return execute(test, defaultTimeoutMs);
    }

    public ExecutionResult execute(TestChromosome test, long timeoutMs) {
        BranchTracker.reset();

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "junitism-test-runner");
            t.setDaemon(true);
            return t;
        });

        try {
            Future<ExecutionResult> future = executor.submit((Callable<ExecutionResult>) () -> {
                if (instrumentingLoader != null) {
                    ExecutionContext.setClassLoader(instrumentingLoader);
                }
                try {
                    Scope scope = new Scope();
                    for (Statement stmt : test.getTestCase().getStatements()) {
                        stmt.execute(scope);
                    }
                    Map<String, Object> returns = new HashMap<>(scope.getVariables());
                    return new ExecutionResult(true, null, BranchTracker.getAndReset(), returns);
                } catch (Throwable t) {
                    return new ExecutionResult(false, t, BranchTracker.getAndReset(), null);
                }
            });

            ExecutionResult result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            test.setLastExecutionData(result.getBranchData());
            test.setTimedOut(result.isTimedOut());
            return result;
        } catch (TimeoutException e) {
            ExecutionResult result = ExecutionResult.timeout(BranchTracker.getAndReset());
            test.setLastExecutionData(result.getBranchData());
            test.setTimedOut(true);
            return result;
        } catch (Exception e) {
            return new ExecutionResult(false, e, BranchTracker.getAndReset(), null);
        } finally {
            executor.shutdownNow();
        }
    }
}
