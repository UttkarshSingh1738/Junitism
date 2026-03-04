package com.junitism.integration;

import com.junitism.analysis.ClasspathScanner;
import com.junitism.analysis.TestCluster;
import com.junitism.ga.DynaMOSA;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JunitismIntegrationTest {

    @Test
    void testClusterBuildsForCalculator() throws Exception {
        Path classes = Path.of("target/classes");
        if (!classes.toFile().exists()) return;

        ClasspathScanner scanner = new ClasspathScanner();
        scanner.scan(List.of(classes));

        TestCluster cluster = new TestCluster(scanner);
        cluster.build(List.of(classes), "com.junitism.testsubjects.Calculator");

        assertFalse(cluster.getTargetMethods().isEmpty());
        assertNotNull(cluster.getTargetType());
    }

    @Test
    void testDynaMOSAGeneratesTests() throws Exception {
        Path classes = Path.of("target/classes");
        if (!classes.toFile().exists()) {
            return;
        }

        DynaMOSA dyna = new DynaMOSA();
        dyna.setTimeBudgetMs(3000);
        dyna.setPopulationSize(5);

        var archive = dyna.run(List.of(classes), "com.junitism.testsubjects.Calculator");
        assertNotNull(archive);
    }
}
