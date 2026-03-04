package com.junitism.cli;

import com.junitism.analysis.ClasspathScanner;
import com.junitism.assertion.AssertionGenerator;
import com.junitism.assertion.AssertionMinimizer;
import com.junitism.ga.Archive;
import com.junitism.ga.DynaMOSA;
import com.junitism.execution.TestExecutor;
import com.junitism.instrumentation.InstrumentingClassLoader;
import com.junitism.output.JUnit5Writer;

import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "junitism", mixinStandardHelpOptions = true)
public class Main implements Runnable {

    @CommandLine.Option(names = {"-cp", "--classpath"}, required = true, description = "Classpath")
    String classpath;

    @CommandLine.Option(names = {"-t", "--target"}, required = true, description = "Target class")
    String target;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Output directory", defaultValue = "./junitism-tests")
    Path outputDir;

    @CommandLine.Option(names = {"-b", "--budget"}, description = "Time budget (seconds)", defaultValue = "60")
    long budgetSeconds;

    @Override
    public void run() {
        try {
            List<Path> classpathEntries = new ArrayList<>();
            for (Path p : ClasspathScanner.parseClasspath(classpath)) {
                classpathEntries.add(p);
            }
            DynaMOSA dyna = new DynaMOSA();
            dyna.setTimeBudgetMs(budgetSeconds * 1000);

            System.out.println("Running DynaMOSA for " + target + "...");
            Archive archive = dyna.run(classpathEntries, target);

            List<com.junitism.testcase.TestCase> testSuite = archive.getFinalTestSuite();
            System.out.println("Generated " + testSuite.size() + " tests");

            ClasspathScanner scanner = new ClasspathScanner();
            scanner.scan(classpathEntries);

            TestExecutor executor = new TestExecutor();
            InstrumentingClassLoader loader = new InstrumentingClassLoader(scanner);
            loader.addTargetClass(target);
            executor.setInstrumentingClassLoader(loader);

            AssertionGenerator assertionGen = new AssertionGenerator(executor);
            AssertionMinimizer minimizer = new AssertionMinimizer();

            List<List<com.junitism.assertion.Assertion>> assertionsPerTest = new ArrayList<>();
            for (var tc : testSuite) {
                var assertions = assertionGen.generate(tc);
                assertions = minimizer.minimize(assertions);
                assertionsPerTest.add(assertions);
            }

            JUnit5Writer writer = new JUnit5Writer();
            String code = writer.write(target, testSuite, assertionsPerTest);

            String simpleName = target.contains(".") ? target.substring(target.lastIndexOf('.') + 1) : target;
            Path outPath = outputDir.resolve(simpleName + "Test.java");
            Files.createDirectories(outPath.getParent());
            Files.writeString(outPath, code);
            System.out.println("Wrote " + outPath);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }
}
