package com.junitism.ga;

import com.junitism.analysis.ClasspathScanner;
import com.junitism.analysis.TestCluster;
import com.junitism.execution.TestExecutor;
import com.junitism.instrumentation.ControlDependencyGraph;
import com.junitism.instrumentation.InstrumentingClassLoader;
import com.junitism.ga.operators.TestCaseCrossover;
import com.junitism.ga.operators.TestCaseMutation;
import com.junitism.testcase.TestCase;
import com.junitism.testcase.TestChromosome;
import com.junitism.testcase.TestFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * DynaMOSA search algorithm: coverage-guided test generation.
 */
public class DynaMOSA {

    private static final Logger log = LoggerFactory.getLogger(DynaMOSA.class);

    private int populationSize = 50;
    private double crossoverRate = 0.75;
    private long timeBudgetMs = 60_000;
    private long randomSeed = System.currentTimeMillis();
    private int tournamentSize = 2;

    private TestCluster cluster;
    private TestExecutor executor;
    private FitnessFunction fitnessFunction;
    private TargetManager targetManager;
    private Archive archive;
    private TestFactory factory;
    private TestCaseCrossover crossover;
    private TestCaseMutation mutation;
    private NonDominatedSorting sorting;
    private PreferenceCriterion preference;
    private Random random;

    public void setPopulationSize(int size) {
        this.populationSize = size;
    }

    public void setCrossoverRate(double rate) {
        this.crossoverRate = rate;
    }

    public void setTimeBudgetMs(long ms) {
        this.timeBudgetMs = ms;
    }

    public void setRandomSeed(long seed) {
        this.randomSeed = seed;
    }

    public Archive run(Iterable<Path> classpath, String targetClassName) throws IOException {
        ClasspathScanner scanner = new ClasspathScanner();
        scanner.scan(classpath);

        cluster = new TestCluster(scanner);
        cluster.build(classpath, targetClassName);

        InstrumentingClassLoader loader = new InstrumentingClassLoader(scanner);
        loader.addTargetClass(targetClassName);

        executor = new TestExecutor();
        executor.setInstrumentingClassLoader(loader);

        Set<BranchCoverageGoal> allGoals = GoalExtractor.extractGoals(targetClassName, scanner);
        ControlDependencyGraph cdg = GoalExtractor.buildCDG(targetClassName, scanner);

        fitnessFunction = new FitnessFunction(cdg);
        targetManager = new TargetManager(cdg, allGoals);
        archive = new Archive();
        random = new Random(randomSeed);
        factory = new TestFactory(cluster, random);
        crossover = new TestCaseCrossover(random);
        crossover.setCrossoverRate(crossoverRate);
        mutation = new TestCaseMutation(random, cluster);
        mutation.setMutationRate(1.0 / Math.max(1, 20));
        sorting = new NonDominatedSorting();
        preference = new PreferenceCriterion();

        List<TestChromosome> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(new TestChromosome(factory.generateRandom()));
        }

        for (TestChromosome tc : population) {
            executor.execute(tc);
            Map<BranchCoverageGoal, Double> fitness = evaluate(tc);
            tc.setFitnessValues(fitness);
            archive.update(tc, fitness);
        }
        targetManager.updateAfterCoverage(archive.getCoveredGoals());

        long startTime = System.currentTimeMillis();
        int generation = 0;

        while (System.currentTimeMillis() - startTime < timeBudgetMs) {
            List<TestChromosome> offspring = new ArrayList<>();
            while (offspring.size() < populationSize) {
                TestChromosome p1 = tournamentSelect(population);
                TestChromosome p2 = tournamentSelect(population);

                TestChromosome c1, c2;
                if (crossover.shouldCrossover()) {
                    var pair = crossover.crossover(p1.getTestCase(), p2.getTestCase());
                    c1 = new TestChromosome(pair.first());
                    c2 = new TestChromosome(pair.second());
                } else {
                    c1 = new TestChromosome(p1.getTestCase().copy());
                    c2 = new TestChromosome(p2.getTestCase().copy());
                }
                c1 = new TestChromosome(mutation.mutate(c1.getTestCase()));
                c2 = new TestChromosome(mutation.mutate(c2.getTestCase()));
                offspring.add(c1);
                offspring.add(c2);
            }

            for (TestChromosome tc : offspring) {
                executor.execute(tc);
                Map<BranchCoverageGoal, Double> fitness = evaluate(tc);
                tc.setFitnessValues(fitness);
                archive.update(tc, fitness);
            }

            targetManager.updateAfterCoverage(archive.getCoveredGoals());

            List<TestChromosome> combined = new ArrayList<>();
            combined.addAll(population);
            combined.addAll(offspring);

            Set<TestChromosome> preferred = preference.getPreferred(combined, targetManager.getCurrentGoals());
            List<List<TestChromosome>> fronts = sorting.sort(combined, targetManager.getCurrentGoals());

            population = selectNextGeneration(fronts, preferred, populationSize);

            generation++;
            if (generation % 10 == 0) {
                log.info("Generation {}: {}/{} goals covered",
                        generation, archive.getCoveredGoals().size(), allGoals.size());
            }

            if (archive.getCoveredGoals().size() >= allGoals.size()) break;
        }

        return archive;
    }

    private Map<BranchCoverageGoal, Double> evaluate(TestChromosome tc) {
        Map<BranchCoverageGoal, Double> result = new HashMap<>();
        for (BranchCoverageGoal goal : targetManager.getCurrentGoals()) {
            result.put(goal, fitnessFunction.evaluate(tc, goal));
        }
        return result;
    }

    private TestChromosome tournamentSelect(List<TestChromosome> population) {
        Set<BranchCoverageGoal> goals = targetManager.getCurrentGoals();
        TestChromosome best = population.get(random.nextInt(population.size()));
        for (int i = 1; i < tournamentSize; i++) {
            TestChromosome candidate = population.get(random.nextInt(population.size()));
            if (dominates(candidate, best, goals)) best = candidate;
        }
        return best;
    }

    private boolean dominates(TestChromosome a, TestChromosome b, Set<BranchCoverageGoal> goals) {
        boolean betterOnAny = false;
        for (BranchCoverageGoal g : goals) {
            double fa = a.getFitness(g);
            double fb = b.getFitness(g);
            if (fa > fb) return false;
            if (fa < fb) betterOnAny = true;
        }
        return betterOnAny;
    }

    private List<TestChromosome> selectNextGeneration(List<List<TestChromosome>> fronts,
                                                     Set<TestChromosome> preferred,
                                                     int n) {
        List<TestChromosome> result = new ArrayList<>(preferred);
        for (List<TestChromosome> front : fronts) {
            for (TestChromosome tc : front) {
                if (result.size() >= n) break;
                if (!result.contains(tc)) result.add(tc);
            }
            if (result.size() >= n) break;
        }
        return result.subList(0, Math.min(n, result.size()));
    }

    public Archive getArchive() {
        return archive;
    }
}
