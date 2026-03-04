# Junitism — Implementation Plan
## Automated JUnit 5 Test Suite Generator for JDK 17+

> **What this is**: A clean-room automated test generator that uses DynaMOSA (the same search-based algorithm EvoSuite uses) rebuilt from scratch with modern bytecode libraries (ASM 9.x, ByteBuddy) to support JDK 17+. Optional LLM augmentation layer (CodaMosa pattern) for plateau escape. Works on bytecode — no source code required.

> **What this document is**: The complete technical blueprint for building Junitism end-to-end. Every section contains enough detail to hand to an engineer (or LLM-agentic coding tool) and produce working code.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Project Structure](#2-project-structure)
3. [Technology Stack](#3-technology-stack)
4. [Phase 1: Class Analysis & TestCluster](#4-phase-1-class-analysis--testcluster)
5. [Phase 2: Bytecode Instrumentation](#5-phase-2-bytecode-instrumentation)
6. [Phase 3: Test Case Representation & Genetic Operators](#6-phase-3-test-case-representation--genetic-operators)
7. [Phase 4: DynaMOSA Search Engine](#7-phase-4-dynamosa-search-engine)
8. [Phase 5: Sandboxed Execution](#8-phase-5-sandboxed-execution)
9. [Phase 6: Assertion Generation](#9-phase-6-assertion-generation)
10. [Phase 7: JUnit 5 Code Output](#10-phase-7-junit-5-code-output)
11. [Phase 8: CodaMosa LLM Augmentation (Optional)](#11-phase-8-codamosa-llm-augmentation-optional)
12. [Phase 9: Build Tool Integration & CLI](#12-phase-9-build-tool-integration--cli)
13. [Phase 10: Enterprise Hardening](#13-phase-10-enterprise-hardening)
14. [Branch Distance Formulas Reference](#14-branch-distance-formulas-reference)
15. [DynaMOSA Algorithm Reference](#15-dynamosa-algorithm-reference)
16. [Configuration Parameters](#16-configuration-parameters)
17. [Testing Strategy for Junitism Itself](#17-testing-strategy-for-junitism-itself)
18. [Academic References](#18-academic-references)

---

## 1. Architecture Overview

```
INPUT                        CORE ENGINE                         OUTPUT
─────                        ───────────                         ──────

.jar / .class files    ┌─────────────────────────────────┐    JUnit 5 .java
     │                 │         DynaMOSA Engine          │    test files
     ▼                 │  ┌───────────┐  ┌────────────┐  │        ▲
┌──────────┐           │  │Population │  │  Archive    │  │        │
│  Class   │──────────►│  │Management │  │ (best test  │──┼───►┌───────────┐
│ Analyzer │  build    │  └─────┬─────┘  │  per branch)│  │    │  Code     │
│ (ASM)    │  test     │        │        └────────────┘  │    │ Generator │
└──────────┘  cluster  │  ┌─────▼─────┐                  │    │(JavaParser│
                       │  │ Fitness    │                  │    │ or String)│
                       │  │ Evaluation │                  │    └───────────┘
                       │  └─────┬─────┘                  │
                       │        │                        │
                       │  ┌─────▼──────────────┐         │
                       │  │ Instrumented Class  │         │
                       │  │ Execution (sandbox) │         │
                       │  └─────┬──────────────┘         │
                       │        │                        │
                       │  ┌─────▼─────┐                  │
                       │  │ Branch     │                  │
                       │  │ Distance   │                  │
                       │  │ Tracker    │                  │
                       │  └───────────┘                  │
                       └─────────────────────────────────┘
                                    ▲
                                    │ (optional, on plateau)
                              ┌─────┴─────┐
                              │  CodaMosa │
                              │ LLM Layer │
                              └───────────┘
```

**Data flow**:
1. ASM reads bytecode from JARs/class files → builds TestCluster (catalog of types, methods, constructors)
2. ASM instruments target class bytecode with branch distance probes
3. DynaMOSA initializes random population of test cases (using TestCluster)
4. Each test case is executed against instrumented code in a sandbox
5. Branch distance tracker reports fitness per test per branch
6. DynaMOSA selects, crosses over, mutates → next generation
7. Best test per branch stored in Archive
8. Repeat until budget exhausted
9. Archive → assertion generation → JUnit 5 code output

---

## 2. Project Structure

```
junitism/
├── pom.xml                              # Parent POM (multi-module Maven)
│
├── junitism-core/                       # Core engine — analysis, instrumentation, search
│   └── src/main/java/com/junitism/
│       ├── analysis/                    # Phase 1: Class analysis
│       │   ├── ClassAnalyzer.java       # ASM-based bytecode analyzer
│       │   ├── TestCluster.java         # Registry of available types/methods
│       │   ├── TypeInfo.java            # Metadata for a discovered type
│       │   ├── MethodInfo.java          # Metadata for a discovered method
│       │   ├── ConstructorInfo.java     # Metadata for a constructor
│       │   ├── DependencyGraph.java     # What types are needed to construct what
│       │   └── ClassHierarchy.java      # Inheritance/interface tree
│       │
│       ├── instrumentation/             # Phase 2: Bytecode instrumentation
│       │   ├── InstrumentingClassLoader.java   # ByteBuddy-backed classloader
│       │   ├── BranchInstrumentingVisitor.java # ASM MethodVisitor for probes
│       │   ├── BranchTracker.java              # Runtime probe data collector
│       │   ├── BranchExecutionData.java        # Per-execution branch data
│       │   ├── ControlFlowGraphBuilder.java    # CFG from bytecode
│       │   └── ControlDependencyGraph.java     # CDG for DynaMOSA targets
│       │
│       ├── testcase/                    # Phase 3: Test case representation
│       │   ├── TestCase.java            # Ordered list of statements
│       │   ├── TestChromosome.java      # GA wrapper around TestCase
│       │   ├── statements/              # Statement types
│       │   │   ├── Statement.java       # Base interface
│       │   │   ├── ConstructorStatement.java
│       │   │   ├── MethodStatement.java
│       │   │   ├── PrimitiveStatement.java
│       │   │   ├── FieldStatement.java
│       │   │   ├── ArrayStatement.java
│       │   │   ├── AssignmentStatement.java
│       │   │   └── NullStatement.java
│       │   ├── VariableReference.java   # Typed reference to a value in the test
│       │   └── VariablePool.java        # Track live variables by type
│       │
│       ├── ga/                          # Phase 4: Genetic algorithm
│       │   ├── DynaMOSA.java            # Main search algorithm
│       │   ├── MOSABase.java            # Shared MOSA/DynaMOSA logic
│       │   ├── Archive.java             # Best test per coverage target
│       │   ├── FitnessFunction.java     # Branch distance + approach level
│       │   ├── BranchCoverageGoal.java  # Single coverage objective
│       │   ├── NonDominatedSorting.java  # NSGA-II fast non-dominated sort
│       │   ├── PreferenceCriterion.java  # DynaMOSA selection preference
│       │   ├── operators/
│       │   │   ├── TestCaseCrossover.java    # Single-point crossover
│       │   │   └── TestCaseMutation.java     # Insert/delete/change mutations
│       │   └── TargetManager.java       # Dynamic target activation via CDG
│       │
│       ├── execution/                   # Phase 5: Sandboxed execution
│       │   ├── TestExecutor.java        # Thread-based executor with timeout
│       │   ├── ExecutionResult.java      # Return values, exceptions, state
│       │   ├── SandboxController.java   # Intercept System.exit, file I/O, etc.
│       │   └── SandboxInstrumentor.java # Bytecode-level method interception
│       │
│       ├── assertion/                   # Phase 6: Assertion generation
│       │   ├── AssertionGenerator.java  # Capture observed state → assertions
│       │   ├── MutationAnalysis.java    # Filter assertions via mutation testing
│       │   └── AssertionMinimizer.java  # Remove redundant assertions
│       │
│       └── output/                      # Phase 7: Code generation
│           ├── JUnit5Writer.java        # Produce .java test files
│           ├── TestFormatter.java       # Naming, formatting, style
│           └── ImportResolver.java      # Manage import statements
│
├── junitism-runtime/                    # Runtime support (on test classpath)
│   └── src/main/java/com/junitism/runtime/
│       ├── BranchTracker.java           # Branch distance probe receiver
│       ├── SandboxController.java       # System.exit intercept handler
│       └── ExecutionTracer.java         # Lightweight execution tracing
│
├── junitism-llm/                        # Phase 8: Optional LLM layer
│   └── src/main/java/com/junitism/llm/
│       ├── CodaMosaController.java      # Plateau detection + LLM dispatch
│       ├── PromptBuilder.java           # Build prompts from uncovered targets
│       ├── LLMClient.java              # Abstract LLM API client
│       ├── ResponseParser.java          # Parse LLM output → TestCase
│       ├── CompilationFeedback.java     # Feed compile errors back to LLM
│       └── providers/
│           ├── OpenAIProvider.java
│           ├── AnthropicProvider.java
│           └── OllamaProvider.java      # Local/self-hosted option
│
├── junitism-cli/                        # Phase 9: Command-line interface
│   └── src/main/java/com/junitism/cli/
│       ├── Main.java                    # Entry point
│       ├── CliParser.java               # Argument parsing
│       └── ReportGenerator.java         # Coverage/generation reports
│
├── junitism-maven-plugin/               # Phase 9: Maven plugin
│   └── src/main/java/com/junitism/maven/
│       └── GenerateMojo.java            # mvn junitism:generate
│
├── junitism-gradle-plugin/              # Phase 9: Gradle plugin
│   └── src/main/java/com/junitism/gradle/
│       └── JunitismPlugin.java          # gradle junitism
│
└── junitism-tests/                      # Integration tests for Junitism itself
    ├── test-subjects/                   # Sample Java classes to generate tests for
    │   ├── Calculator.java
    │   ├── StringUtils.java
    │   ├── OrderService.java
    │   ├── BinarySearchTree.java
    │   └── ComplexHierarchy.java
    └── src/test/java/com/junitism/
        ├── integration/
        └── benchmark/
```

---

## 3. Technology Stack

| Component | Library | Version | License | Purpose |
|-----------|---------|---------|---------|---------|
| Bytecode reading & probes | ASM | 9.7+ | BSD-3-Clause | Read .class files, insert branch distance probes |
| Classloading & manipulation | ByteBuddy | 1.15+ | Apache-2.0 | Custom classloader, JPMS handling, agent support |
| Coverage measurement | JaCoCo | 0.8.12+ | EPL-2.0 | Line/branch coverage reporting (complementary to our probes) |
| Code generation | JavaParser | 3.26+ | Apache-2.0 | Generate JUnit 5 .java files, optional source analysis |
| Build system | Maven | 3.9+ | Apache-2.0 | Multi-module build |
| CLI framework | Picocli | 4.7+ | Apache-2.0 | Command-line argument parsing |
| Logging | SLF4J + Logback | 2.x / 1.5+ | MIT / EPL-1.0 | Structured logging |
| Testing (self) | JUnit 5 | 5.10+ | EPL-2.0 | Test Junitism itself |
| JSON (config/reports) | Jackson | 2.17+ | Apache-2.0 | Configuration files, JSON reports |

**All licenses are permissive or weak-copyleft. No LGPL/GPL dependencies. Safe for enterprise distribution.**

**Java version**: JDK 17 minimum, target compatibility up to JDK 21+.

---

## 4. Phase 1: Class Analysis & TestCluster

**Goal**: Given a classpath and a target class name, build a complete catalog of everything needed to construct and invoke that class.

### 4.1 ClassAnalyzer (ASM-based)

The analyzer works entirely on bytecode. No source required.

```
Input:  classpath (List<Path>)  +  target class name (String)
Output: TestCluster
```

**Implementation using ASM ClassVisitor**:

```java
// Pseudocode for ClassAnalyzer
public class ClassAnalyzer extends ClassVisitor {

    // visitMethod() captures:
    //   - method name, descriptor (parameter types, return type)
    //   - access flags (public/private/static/abstract)
    //   - exceptions declared

    // visitField() captures:
    //   - field name, type, access flags

    // visit() captures:
    //   - superclass, interfaces
    //   - class access flags (abstract, final, sealed, record, enum)
}
```

**Key information to extract per class**:
- All public constructors with parameter types
- All public methods with parameter types, return types, exception types
- All public fields (for direct state inspection in assertions)
- Superclass chain and implemented interfaces
- Whether the class is: abstract, final, sealed, a record, an enum, an interface
- Inner classes / static nested classes

### 4.2 TestCluster

The TestCluster is the central registry that answers: "What types exist, and how can I create instances of them?"

```java
public class TestCluster {

    // For each type T, store:
    //   - All constructors that produce T
    //   - All static factory methods that return T
    //   - All methods on any type that return T (generators)
    //   - Whether T is a primitive, enum, array, or interface
    Map<TypeInfo, List<ConstructorInfo>> constructors;
    Map<TypeInfo, List<MethodInfo>> generators;  // methods returning this type

    // For the target class:
    //   - All methods to test (coverage targets come from these)
    List<MethodInfo> targetMethods;

    // Dependency resolution:
    //   - To call method M on class C, I need an instance of C
    //   - To construct C, I need instances of its constructor parameter types
    //   - Recursively resolve until all types can be constructed
    DependencyGraph dependencies;
}
```

**Building the TestCluster**:
1. Start with the target class
2. Extract all its constructors → note their parameter types
3. For each parameter type, find its constructors → note their parameter types
4. Repeat recursively until all types are resolvable or we hit a dead end
5. For dead ends (interfaces, abstract classes with no known concrete subtypes on the classpath), mark as "needs mock or null"
6. Scan the classpath for concrete implementations of interfaces/abstract classes

**Handling common types**:
- `String` → generate random strings, empty string, null
- `int`, `long`, `double`, etc. → random values, boundary values (0, 1, -1, MAX, MIN)
- `List`, `Set`, `Map` → `ArrayList`, `HashSet`, `HashMap` with random contents
- `enum` types → pick random enum constants
- Arrays → create with random length and random elements

### 4.3 Classpath Scanning

```java
// Load classes from JARs, directories, or individual .class files
public class ClasspathScanner {
    // Given: List<Path> classpath entries
    // For JARs: open as ZipFile, iterate entries ending in .class
    // For directories: walk file tree for .class files
    // For each .class: run through ClassAnalyzer

    // Build a Map<String, byte[]> className → bytecode
    // This is the source of truth for all class data
}
```

---

## 5. Phase 2: Bytecode Instrumentation

**Goal**: Rewrite target class bytecode at load time to insert probes that capture branch distance values.

### 5.1 InstrumentingClassLoader

Uses ByteBuddy to intercept class loading and apply ASM transformations:

```java
public class InstrumentingClassLoader extends ClassLoader {

    private final ClasspathScanner scanner;       // source of bytecode
    private final Set<String> targetPackages;     // what to instrument

    @Override
    protected Class<?> findClass(String name) {
        byte[] original = scanner.getBytecode(name);
        if (shouldInstrument(name)) {
            byte[] instrumented = instrument(original);
            return defineClass(name, instrumented, 0, instrumented.length);
        }
        return defineClass(name, original, 0, original.length);
    }

    private byte[] instrument(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                // CRITICAL: Override to use our classloader for type resolution
                // ClassWriter.COMPUTE_FRAMES calls this internally
                // Default impl uses Class.forName() which fails for target classes
                return resolveViaOurClassloader(type1, type2);
            }
        };
        cr.accept(new BranchInstrumentingClassVisitor(Opcodes.ASM9, cw),
                  ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }
}
```

**JPMS handling**: When the target project uses Java modules, pass these JVM flags:
```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/java.lang.invoke=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.io=ALL-UNNAMED
```
Junitism should auto-detect if modules are present and configure flags accordingly.

### 5.2 BranchInstrumentingVisitor (ASM MethodVisitor)

This is the core instrumentation. For every branch instruction in bytecode, we duplicate the operands and call `BranchTracker` before the branch executes.

**Branch instruction types to handle**:

| Bytecode | Stack Before | What It Tests |
|----------|-------------|---------------|
| `IFEQ` | `..., value` | `value == 0` |
| `IFNE` | `..., value` | `value != 0` |
| `IFLT` | `..., value` | `value < 0` |
| `IFGE` | `..., value` | `value >= 0` |
| `IFGT` | `..., value` | `value > 0` |
| `IFLE` | `..., value` | `value <= 0` |
| `IF_ICMPEQ` | `..., val1, val2` | `val1 == val2` |
| `IF_ICMPNE` | `..., val1, val2` | `val1 != val2` |
| `IF_ICMPLT` | `..., val1, val2` | `val1 < val2` |
| `IF_ICMPGE` | `..., val1, val2` | `val1 >= val2` |
| `IF_ICMPGT` | `..., val1, val2` | `val1 > val2` |
| `IF_ICMPLE` | `..., val1, val2` | `val1 <= val2` |
| `IFNULL` | `..., ref` | `ref == null` |
| `IFNONNULL` | `..., ref` | `ref != null` |
| `IF_ACMPEQ` | `..., ref1, ref2` | `ref1 == ref2` |
| `IF_ACMPNE` | `..., ref1, ref2` | `ref1 != ref2` |
| `TABLESWITCH` | `..., index` | switch (contiguous cases) |
| `LOOKUPSWITCH` | `..., key` | switch (sparse cases) |

**Instrumentation pattern for two-operand integer comparisons**:

```java
@Override
public void visitJumpInsn(int opcode, Label label) {
    int branchId = branchCounter++;

    if (isBinaryIntComparison(opcode)) {
        // Stack: ..., val1, val2
        mv.visitInsn(Opcodes.DUP2);
        // Stack: ..., val1, val2, val1, val2
        mv.visitLdcInsn(branchId);
        mv.visitLdcInsn(opcode);
        mv.visitLdcInsn(className + "#" + methodName);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
            "com/junitism/runtime/BranchTracker",
            "trackBinaryBranch",
            "(IIIILjava/lang/String;)V", false);
        // Stack: ..., val1, val2  (original values for the real branch)
    }

    super.visitJumpInsn(opcode, label);  // emit original branch
}
```

**Handling long/float/double comparisons**:

Java compiles `if (longA > longB)` as: `LCMP` → `IFGT`. The `LCMP` consumes both longs and pushes -1/0/1. To get branch distance, we must intercept **before** `LCMP` and capture the actual long values. Implementation:

1. Track when we see `LCMP`, `DCMPG`, `DCMPL`, `FCMPG`, `FCMPL`
2. On the **next** `IFxx` instruction, we know the preceding comparison type
3. Insert probes that capture the original operands before the comparison instruction

### 5.3 BranchTracker (Runtime Component)

Lives in `junitism-runtime` module, must be on the classpath when instrumented code runs:

```java
public class BranchTracker {
    // Thread-local to support parallel test execution
    private static final ThreadLocal<BranchExecutionData> currentExecution =
        ThreadLocal.withInitial(BranchExecutionData::new);

    public static void reset() {
        currentExecution.set(new BranchExecutionData());
    }

    public static BranchExecutionData getAndReset() {
        BranchExecutionData data = currentExecution.get();
        currentExecution.set(new BranchExecutionData());
        return data;
    }

    public static void trackBinaryBranch(int val1, int val2, int branchId,
                                          int opcode, String location) {
        double trueDist = computeTrueDistance(val1, val2, opcode);
        double falseDist = computeFalseDistance(val1, val2, opcode);
        currentExecution.get().record(branchId, location, trueDist, falseDist);
    }

    // See Section 14 for all distance formulas
}
```

**BranchExecutionData** stores:
```java
public class BranchExecutionData {
    // For each branch encountered during execution:
    //   branchId → (trueDistance, falseDistance, wasExecuted, actualOutcome)
    Map<Integer, BranchRecord> branches = new HashMap<>();

    // For approach level computation:
    Set<Integer> executedBranches = new HashSet<>();
}
```

### 5.4 Control Flow Graph (CFG) Construction

Built from bytecode using ASM, needed for approach level computation:

```java
public class ControlFlowGraphBuilder extends MethodVisitor {
    // Nodes: basic blocks (sequences of instructions with no jumps)
    // Edges: jump targets, fall-through, exception handlers
    //
    // For each method, produce:
    //   - List<BasicBlock> nodes
    //   - Map<BasicBlock, List<BasicBlock>> successors
    //   - Map<BasicBlock, List<BasicBlock>> predecessors
    //   - Map<Integer, BasicBlock> branchId → containing block
}
```

### 5.5 Control Dependency Graph (CDG)

Built from the CFG, required by DynaMOSA for dynamic target selection:

```java
public class ControlDependencyGraph {
    // Node B is control-dependent on node A if:
    //   A has two successors (it's a branch), and
    //   B is reachable from one successor but not the other
    //   (informally: A's branch outcome determines whether B executes)
    //
    // Construction: standard algorithm via post-dominator tree
    //   1. Build post-dominator tree of the CFG
    //   2. For each edge (A→B) in CFG:
    //      if B does not post-dominate A, then:
    //        all nodes on the path from B to the post-dominator of A
    //        in the post-dominator tree are control-dependent on A
    //
    // Output: Map<BranchId, List<BranchId>> dependents
    //   "Branch X is control-dependent on Branch Y"
    //   Meaning: Branch Y must be covered before Branch X can be reached

    Map<Integer, Set<Integer>> getStructuralChildren(int branchId);
    Set<Integer> getRootBranches();  // branches with no dependencies
}
```

---

## 6. Phase 3: Test Case Representation & Genetic Operators

### 6.1 Test Case Structure

A test case is an ordered sequence of typed statements:

```java
public class TestCase {
    List<Statement> statements;    // ordered sequence
    VariablePool variables;        // track live typed references

    // Example test case internally:
    // [0] ConstructorStatement: var0:InventoryRepo = new InventoryRepo()
    // [1] ConstructorStatement: var1:PaymentGateway = new PaymentGateway()
    // [2] ConstructorStatement: var2:OrderService = new OrderService(var0, var1)
    // [3] PrimitiveStatement:   var3:double = 99.95
    // [4] PrimitiveStatement:   var4:int = 3
    // [5] MethodStatement:      var5:double = var2.applyDiscount(var3, var4)
}
```

### 6.2 Statement Types

```java
interface Statement {
    VariableReference getReturnValue();  // what this statement produces
    List<VariableReference> getInputs(); // what this statement consumes
    Statement copy();                    // deep copy for crossover/mutation
    void execute(Scope scope);           // reflective execution
}

// ConstructorStatement: var = new Type(arg1, arg2, ...)
// MethodStatement:      var = target.method(arg1, arg2, ...) or void
// PrimitiveStatement:   var = <literal value>  (int, long, double, float, boolean, char, String)
// FieldStatement:       var = target.field  or  target.field = var
// ArrayStatement:       var = new Type[length]  or  var[index] = value
// NullStatement:        var = null  (typed null for reference parameters)
// EnumStatement:        var = EnumType.CONSTANT
```

### 6.3 VariableReference

```java
public class VariableReference {
    int position;        // index in the statement list where this was created
    TypeInfo type;       // the declared type
    String name;         // generated name for code output (e.g., "orderService0")

    // A variable is "live" from its creation statement until:
    //   - the end of the test, or
    //   - it's overwritten by a new assignment to the same name
    // Variables can only be used by statements AFTER their creation
}
```

### 6.4 VariablePool

```java
public class VariablePool {
    // Given a type T and a position P in the statement list:
    //   return all VariableReferences of type T (or subtype) created before P
    // This is how mutation/crossover finds valid arguments for method calls
    List<VariableReference> getVariablesOfType(TypeInfo type, int beforePosition);
}
```

### 6.5 Crossover: Single-Point

```java
public class TestCaseCrossover {
    double crossoverRate = 0.75;  // probability of applying crossover

    Pair<TestCase, TestCase> crossover(TestCase parent1, TestCase parent2) {
        // 1. Choose random cut point in parent1 (position α)
        // 2. Choose random cut point in parent2 (position β)
        // 3. Offspring1 = parent1[0..α] + parent2[β..end]
        // 4. Offspring2 = parent2[0..β] + parent1[α..end]
        // 5. Fix variable references: if offspring references a variable
        //    that no longer exists (from the other parent's prefix),
        //    either re-map to a compatible variable or drop the statement
    }
}
```

**Critical detail**: After crossover, variable references in the appended portion may point to variables that don't exist in the new prefix. Resolution strategy:
- Try to find a variable of the same type in the new prefix
- If none exists, insert a constructor/primitive statement to create one
- If the type can't be constructed, drop the statement

### 6.6 Mutation Operators

Applied to each statement in a test case independently, with configurable probabilities:

```java
public class TestCaseMutation {
    double mutationRate = 1.0 / testLength;  // per-statement probability

    TestCase mutate(TestCase original) {
        TestCase mutated = original.copy();

        for (int i = 0; i < mutated.size(); i++) {
            if (random.nextDouble() < mutationRate) {
                // Choose one mutation operator:
                switch (randomChoice()) {
                    case INSERT:   insertRandomStatement(mutated, i); break;
                    case DELETE:   deleteStatement(mutated, i); break;
                    case CHANGE:   changeStatementParameter(mutated, i); break;
                    case REPLACE:  replaceWithDifferentCall(mutated, i); break;
                }
            }
        }
        return mutated;
    }
}
```

**INSERT**: Add a random statement (constructor call, method call, or primitive) at position i. The method/constructor is chosen from the TestCluster. Arguments are drawn from the VariablePool (variables available at position i).

**DELETE**: Remove statement i. Then remove any later statements that reference the deleted statement's output variable (cascading delete). Be careful not to delete the only constructor of the target class.

**CHANGE**: For a method/constructor call, replace one of its arguments:
- If the argument is a primitive: perturb the value (add/subtract small delta, flip boolean, change character)
- If the argument is an object reference: swap to a different variable of the same type from the pool
- If the argument is null: replace with a constructed instance, or vice versa

**REPLACE**: Replace the method call with a different method on the same object that has the same or compatible parameter types. e.g., replace `list.add(x)` with `list.remove(x)`.

### 6.7 Test Factory

Responsible for creating random test cases for population initialization:

```java
public class TestFactory {
    TestCluster cluster;
    int maxStatements = 80;  // max statements per test case

    TestCase generateRandom() {
        TestCase tc = new TestCase();
        int targetLength = random.nextInt(maxStatements) + 1;

        while (tc.size() < targetLength) {
            // 1. Pick a random method from the target class
            MethodInfo method = cluster.getRandomTargetMethod();

            // 2. Ensure we have an instance of the target class
            //    If not, insert constructor chain
            ensureInstanceExists(tc, method.getDeclaringClass());

            // 3. Ensure we have arguments for the method
            for (TypeInfo paramType : method.getParameterTypes()) {
                ensureInstanceExists(tc, paramType);
            }

            // 4. Add the method call statement
            tc.addStatement(new MethodStatement(method, ...));
        }
        return tc;
    }

    void ensureInstanceExists(TestCase tc, TypeInfo type) {
        // If the VariablePool already has a variable of this type, done
        // Otherwise, find a constructor for this type
        // Recursively ensure constructor parameters exist
        // Add constructor statement(s) to the test case
        // Handle cycles (A needs B, B needs A) by setting a recursion depth limit
    }
}
```

---

## 7. Phase 4: DynaMOSA Search Engine

### 7.1 Algorithm Overview

DynaMOSA (Dynamic Many-Objective Sorting Algorithm) from Panichella et al., IEEE TSE 2018:

```
ALGORITHM: DynaMOSA

INPUT:
  - Target class C
  - TestCluster (from Phase 1)
  - Instrumented bytecode (from Phase 2)
  - Time budget (seconds)
  - Population size N (default: 50)

OUTPUT:
  - Archive: Map<BranchCoverageGoal, TestCase>

PROCEDURE:
  1. Extract all branch coverage goals from C
     (each branch = two goals: true branch and false branch)

  2. Build Control Dependency Graph (CDG) from CFGs

  3. Identify root goals: branches with no control dependencies
     (these are always "active" — they can be reached unconditionally)

  4. Initialize currentGoals = root goals

  5. Generate initial population P of N random test cases (via TestFactory)

  6. Evaluate fitness of each test in P against all currentGoals
     (execute against instrumented code, collect branch distances)

  7. Update archive: for each goal, store the test with best fitness

  8. WHILE time budget not exhausted:
     a. Select parents from P using tournament selection
        with NSGA-II ranking on currentGoals + preference criterion

     b. Apply crossover and mutation to produce offspring Q

     c. Evaluate fitness of Q against all currentGoals

     d. Update archive with any improved tests from Q

     e. Check for newly covered goals in archive
        → For each newly covered goal G:
            Activate G's structural children from CDG
            (add them to currentGoals)

     f. Form next generation:
        Combined = P ∪ Q
        Rank combined population using non-dominated sorting on currentGoals
        Select best N individuals → new P

  9. Return archive
```

### 7.2 Dynamic Target Selection (The "Dyn" in DynaMOSA)

This is the key innovation over plain MOSA:

```java
public class TargetManager {
    ControlDependencyGraph cdg;
    Set<BranchCoverageGoal> allGoals;
    Set<BranchCoverageGoal> currentGoals;   // currently active
    Set<BranchCoverageGoal> coveredGoals;   // already covered

    void initialize() {
        // Only root branches are initially active
        currentGoals = cdg.getRootBranches().stream()
            .flatMap(b -> goalsForBranch(b))
            .collect(toSet());
    }

    void updateAfterCoverage(BranchCoverageGoal newlyCovered) {
        coveredGoals.add(newlyCovered);

        // Activate structural children:
        // If branch B depends on branch A, and A was just covered,
        // then B becomes reachable → add B's goals to currentGoals
        for (BranchCoverageGoal child : cdg.getStructuralChildren(newlyCovered)) {
            if (!coveredGoals.contains(child)) {
                currentGoals.add(child);
            }
        }
    }
}
```

**Why this matters**: Consider method with 20 branches where branch 15 is deep inside nested ifs. Without dynamic selection, the algorithm tries to optimize for all 40 goals (20 branches × 2 sides) simultaneously — most of which are unreachable. With dynamic selection, it starts with only the root branches (maybe 4 goals), and progressively activates deeper branches as outer ones are covered. This focuses the search and dramatically improves efficiency.

### 7.3 Non-Dominated Sorting (NSGA-II)

Used to rank the population for selection:

```java
public class NonDominatedSorting {

    // Test A dominates Test B if:
    //   A is <= B on ALL active goals, AND
    //   A is strictly < B on AT LEAST ONE active goal
    // (lower fitness = better)

    List<List<TestChromosome>> sort(List<TestChromosome> population,
                                     Set<BranchCoverageGoal> activeGoals) {
        // Fast non-dominated sort (Deb et al., 2002):
        // 1. For each individual, count how many others dominate it
        // 2. Individuals dominated by nobody → Front 0 (best)
        // 3. Remove Front 0, repeat → Front 1, Front 2, ...
        // Returns list of fronts
    }
}
```

### 7.4 Preference Criterion

DynaMOSA adds a preference criterion on top of NSGA-II:

```java
public class PreferenceCriterion {
    // For each active goal G:
    //   The test with the BEST fitness for G gets "preference"
    //   Preferred tests are always kept in the population (elitism)
    //
    // This ensures that the best test for each specific branch
    // survives into the next generation, even if it's dominated
    // on other objectives.

    Set<TestChromosome> getPreferred(List<TestChromosome> population,
                                      Set<BranchCoverageGoal> activeGoals) {
        Set<TestChromosome> preferred = new HashSet<>();
        for (BranchCoverageGoal goal : activeGoals) {
            TestChromosome best = population.stream()
                .min(Comparator.comparingDouble(t -> t.getFitness(goal)))
                .orElse(null);
            if (best != null) preferred.add(best);
        }
        return preferred;
    }
}
```

### 7.5 Fitness Function

```java
public class FitnessFunction {

    // Combined fitness for a test case T and a branch coverage goal G:
    //
    //   fitness(T, G) = approach_level(T, G) + normalize(branch_distance(T, G))
    //
    //   Range: [0, ∞)  where 0 = goal covered
    //
    //   approach_level: number of control-dependent nodes between
    //                   where T diverged and the target branch
    //                   (0 if T reached the target branch's predicate)
    //
    //   branch_distance: how close the predicate was to going the desired way
    //                    (0 if it went the desired way)
    //
    //   normalize(d) = d / (d + 1)    maps [0, ∞) to [0, 1)

    double evaluate(TestChromosome test, BranchCoverageGoal goal) {
        BranchExecutionData data = test.getLastExecutionData();

        if (data.wasBranchExecuted(goal.getBranchId())) {
            // Approach level = 0, test reached this branch
            double branchDist = data.getBranchDistance(goal.getBranchId(), goal.isTrue());
            return normalize(branchDist);  // range [0, 1)
        } else {
            // Compute approach level via CDG
            int approachLevel = computeApproachLevel(data, goal);
            // Branch distance = 1.0 (we never reached the predicate)
            return approachLevel + 1.0;
        }
    }

    int computeApproachLevel(BranchExecutionData data, BranchCoverageGoal goal) {
        // Walk the CDG from the goal upward
        // Count how many control-dependent ancestors were NOT executed
        // The approach level = number of unexecuted ancestors
        // between the deepest executed ancestor and the goal
    }

    double normalize(double distance) {
        return distance / (distance + 1.0);
    }
}
```

### 7.6 Archive

```java
public class Archive {
    // For each coverage goal, store the best test case seen so far
    Map<BranchCoverageGoal, TestChromosome> bestTests = new HashMap<>();

    void update(TestChromosome test, Map<BranchCoverageGoal, Double> fitnessValues) {
        for (Map.Entry<BranchCoverageGoal, Double> entry : fitnessValues.entrySet()) {
            BranchCoverageGoal goal = entry.getKey();
            double fitness = entry.getValue();

            if (fitness == 0.0) {  // goal is covered
                TestChromosome current = bestTests.get(goal);
                if (current == null || test.size() < current.size()) {
                    // Prefer shorter tests (minimization)
                    bestTests.put(goal, test.copy());
                }
            }
        }
    }

    Set<BranchCoverageGoal> getCoveredGoals() {
        return bestTests.keySet();
    }

    List<TestCase> getFinalTestSuite() {
        // Remove duplicate tests (one test may cover multiple goals)
        // Return minimal set of tests covering all achieved goals
        return minimize(new ArrayList<>(bestTests.values()));
    }
}
```

### 7.7 Main Loop Implementation

```java
public class DynaMOSA {
    int populationSize = 50;
    double crossoverRate = 0.75;
    double mutationRate;  // 1/avg_test_length
    long timeBudgetMs = 60_000;  // 60 seconds per class

    public Archive run(ClassUnderTest target) {
        // Setup
        TestCluster cluster = new ClassAnalyzer().analyze(target);
        Set<BranchCoverageGoal> allGoals = extractGoals(target);
        ControlDependencyGraph cdg = buildCDG(target);
        TargetManager targets = new TargetManager(cdg, allGoals);
        Archive archive = new Archive();
        TestFactory factory = new TestFactory(cluster);
        TestExecutor executor = new TestExecutor();  // sandboxed

        // Initialize
        List<TestChromosome> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(new TestChromosome(factory.generateRandom()));
        }

        // Evaluate initial population
        for (TestChromosome tc : population) {
            executor.execute(tc);
            Map<BranchCoverageGoal, Double> fitness = evaluate(tc, targets.getCurrentGoals());
            tc.setFitnessValues(fitness);
            archive.update(tc, fitness);
        }
        targets.updateAfterCoverage(archive.getCoveredGoals());

        // Main loop
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeBudgetMs) {

            // Generate offspring
            List<TestChromosome> offspring = new ArrayList<>();
            while (offspring.size() < populationSize) {
                TestChromosome parent1 = tournamentSelect(population, targets.getCurrentGoals());
                TestChromosome parent2 = tournamentSelect(population, targets.getCurrentGoals());

                TestChromosome child1, child2;
                if (random.nextDouble() < crossoverRate) {
                    var pair = crossover.crossover(parent1, parent2);
                    child1 = pair.first();
                    child2 = pair.second();
                } else {
                    child1 = parent1.copy();
                    child2 = parent2.copy();
                }

                child1 = mutation.mutate(child1);
                child2 = mutation.mutate(child2);
                offspring.add(child1);
                offspring.add(child2);
            }

            // Evaluate offspring
            for (TestChromosome tc : offspring) {
                executor.execute(tc);
                Map<BranchCoverageGoal, Double> fitness = evaluate(tc, targets.getCurrentGoals());
                tc.setFitnessValues(fitness);
                archive.update(tc, fitness);
            }

            // Update targets (activate structural children of newly covered goals)
            targets.updateAfterCoverage(archive.getCoveredGoals());

            // Survivor selection
            List<TestChromosome> combined = new ArrayList<>();
            combined.addAll(population);
            combined.addAll(offspring);

            // Preference criterion: always keep best test per active goal
            Set<TestChromosome> preferred = getPreferred(combined, targets.getCurrentGoals());

            // NSGA-II ranking on remaining
            List<List<TestChromosome>> fronts = nonDominatedSort(combined, targets.getCurrentGoals());

            // Fill next population: preferred first, then by front, then by crowding distance
            population = selectNextGeneration(fronts, preferred, populationSize);

            // Logging
            log.info("Generation {}: {}/{} goals covered",
                generation++, archive.getCoveredGoals().size(), allGoals.size());

            // Early termination if all goals covered
            if (archive.getCoveredGoals().size() == allGoals.size()) break;
        }

        return archive;
    }
}
```

---

## 8. Phase 5: Sandboxed Execution

**Goal**: Execute generated test cases safely. Prevent System.exit(), infinite loops, file system damage, network access.

### 8.1 SecurityManager is Dead — Use Bytecode Interception

`SecurityManager` is deprecated in JDK 17 (JEP 411) and permanently disabled in JDK 24 (JEP 486). Replace with bytecode-level interception:

```java
public class SandboxInstrumentor extends MethodVisitor {
    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
                                 String descriptor, boolean isInterface) {
        // Intercept System.exit(int)
        if (owner.equals("java/lang/System") && name.equals("exit")) {
            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                "com/junitism/runtime/SandboxController",
                "interceptExit", "(I)V", false);
            return;
        }

        // Intercept Runtime.halt(int)
        if (owner.equals("java/lang/Runtime") && name.equals("halt")) {
            mv.visitInsn(Opcodes.POP); // remove Runtime reference
            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                "com/junitism/runtime/SandboxController",
                "interceptHalt", "(I)V", false);
            return;
        }

        // Intercept new FileOutputStream (file writes)
        // Intercept Socket construction (network)
        // Intercept ProcessBuilder.start() (process spawning)

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
```

### 8.2 Thread-Based Execution with Timeout

```java
public class TestExecutor {
    long defaultTimeoutMs = 5000;  // 5 seconds per test

    public ExecutionResult execute(TestChromosome test) {
        BranchTracker.reset();

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "junitism-test-runner");
            t.setDaemon(true);
            return t;
        });

        try {
            Future<ExecutionResult> future = executor.submit(() -> {
                try {
                    // Execute each statement in the test via reflection
                    Scope scope = new Scope();
                    for (Statement stmt : test.getTestCase().getStatements()) {
                        stmt.execute(scope);
                    }
                    return new ExecutionResult(true, null,
                        BranchTracker.getAndReset(), scope.getReturnValues());
                } catch (Throwable t) {
                    return new ExecutionResult(false, t,
                        BranchTracker.getAndReset(), null);
                }
            });

            return future.get(defaultTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return ExecutionResult.timeout(BranchTracker.getAndReset());
        } finally {
            executor.shutdownNow();
        }
    }
}
```

### 8.3 Reflective Test Execution

Each statement executes via reflection:

```java
// ConstructorStatement.execute(scope):
Constructor<?> ctor = clazz.getDeclaredConstructor(paramTypes);
ctor.setAccessible(true);
Object instance = ctor.newInstance(resolveArgs(scope));
scope.setVariable(returnVar, instance);

// MethodStatement.execute(scope):
Method method = clazz.getDeclaredMethod(name, paramTypes);
method.setAccessible(true);
Object target = scope.getVariable(targetVar);
Object result = method.invoke(target, resolveArgs(scope));
scope.setVariable(returnVar, result);
```

---

## 9. Phase 6: Assertion Generation

**Goal**: Turn observed behavior into meaningful JUnit 5 assertions.

### 9.1 Observation-Based Assertions

After the final test suite is determined (from the Archive), re-execute each test and capture:

```java
public class AssertionGenerator {

    List<Assertion> generate(TestCase test) {
        ExecutionResult result = executor.execute(test);
        List<Assertion> assertions = new ArrayList<>();

        for (MethodStatement stmt : test.getMethodStatements()) {
            Object returnValue = result.getReturnValue(stmt);

            if (returnValue != null) {
                if (isPrimitive(returnValue) || returnValue instanceof String) {
                    // assertEquals(expectedValue, actual)
                    assertions.add(new EqualsAssertion(stmt.getReturnVar(), returnValue));
                } else {
                    // assertNotNull(actual)
                    assertions.add(new NotNullAssertion(stmt.getReturnVar()));
                }
            } else if (stmt.getReturnType() != void.class) {
                // assertNull(actual)
                assertions.add(new NullAssertion(stmt.getReturnVar()));
            }

            // If the method was expected to throw an exception:
            Throwable thrown = result.getException(stmt);
            if (thrown != null) {
                assertions.add(new ThrowsAssertion(stmt, thrown.getClass()));
            }
        }

        // Object state assertions: check fields of the target object
        Object targetInstance = result.getTargetInstance();
        for (Field field : getInspectableFields(targetInstance)) {
            Object value = field.get(targetInstance);
            assertions.add(new FieldAssertion(field, value));
        }

        return assertions;
    }
}
```

### 9.2 Assertion Minimization

Remove assertions that don't add fault-detection value:

```java
public class AssertionMinimizer {
    List<Assertion> minimize(TestCase test, List<Assertion> assertions) {
        // 1. Remove redundant assertions:
        //    If assertNotNull(x) and assertEquals(5, x), drop the assertNotNull

        // 2. Remove unstable assertions:
        //    Re-run test 3 times. If an assertion value changes, drop it.
        //    (e.g., assertions on timestamps, random values, hashCodes)

        // 3. Optional: Mutation-based filtering
        //    Create simple mutants of the target class (negate a condition, etc.)
        //    Keep only assertions that detect at least one mutant
        //    (expensive but produces higher-quality assertions)
    }
}
```

---

## 10. Phase 7: JUnit 5 Code Output

**Goal**: Convert the internal TestCase representation into compilable, readable JUnit 5 `.java` files.

### 10.1 JUnit5Writer

Uses JavaParser to build the AST programmatically:

```java
public class JUnit5Writer {

    public String write(String targetClassName, List<TestCase> testSuite) {
        CompilationUnit cu = new CompilationUnit();

        // Package
        cu.setPackageDeclaration(deriveTestPackage(targetClassName));

        // Imports
        cu.addImport("org.junit.jupiter.api.Test");
        cu.addImport("org.junit.jupiter.api.BeforeEach");
        cu.addImport("org.junit.jupiter.api.DisplayName");
        cu.addImport("static org.junit.jupiter.api.Assertions.*");
        // Add imports for target class and dependencies

        // Test class
        ClassOrInterfaceDeclaration testClass = cu.addClass(
            simpleClassName(targetClassName) + "Test");

        // Generate test methods
        for (int i = 0; i < testSuite.size(); i++) {
            TestCase tc = testSuite.get(i);
            MethodDeclaration testMethod = testClass.addMethod(
                "test" + describeGoal(tc),
                com.github.javaparser.ast.Modifier.Keyword.PUBLIC);

            testMethod.addAnnotation("Test");
            testMethod.addAnnotation(new NormalAnnotationExpr(
                new Name("DisplayName"),
                new NodeList<>(new MemberValuePair("value",
                    new StringLiteralExpr(describeWhatTestDoes(tc))))));

            // Body: convert each Statement → Java source
            BlockStmt body = new BlockStmt();
            for (Statement stmt : tc.getStatements()) {
                body.addStatement(stmtToJavaParser(stmt));
            }

            // Assertions
            for (Assertion assertion : tc.getAssertions()) {
                body.addStatement(assertionToJavaParser(assertion));
            }

            testMethod.setBody(body);
        }

        return cu.toString();  // JavaParser pretty-prints
    }
}
```

### 10.2 Variable Naming

Don't produce `var0`, `var1`. Generate readable names:

```java
public class TestFormatter {
    String generateVariableName(TypeInfo type, int index) {
        // "OrderService" → "orderService0"
        // "List<Item>" → "itemList0"
        // "int" → "intValue0"
        // "String" → "string0"
        String base = decapitalize(type.getSimpleName());
        return base + index;
    }
}
```

### 10.3 Output Structure

```
output-directory/
└── com/example/
    ├── OrderServiceTest.java
    ├── CalculatorTest.java
    └── StringUtilsTest.java
```

Mirror the package structure of the target classes. One test file per target class.

---

## 11. Phase 8: CodaMosa LLM Augmentation (Optional)

**Goal**: When the DynaMOSA search plateaus (no new coverage for N generations), query an LLM for targeted test cases to escape the plateau.

**This entire phase is optional. The DynaMOSA engine works fully without it.**

### 11.1 Plateau Detection

```java
public class CodaMosaController {
    int plateauThreshold = 20;  // generations without new coverage
    int generationsSinceLastCoverage = 0;

    boolean isPlateaued() {
        return generationsSinceLastCoverage >= plateauThreshold;
    }

    void onNewCoverage() {
        generationsSinceLastCoverage = 0;
    }

    void onGenerationComplete() {
        generationsSinceLastCoverage++;
    }
}
```

### 11.2 Prompt Construction

When plateaued, identify uncovered goals and build a prompt:

```java
public class PromptBuilder {

    String buildPrompt(MethodInfo uncoveredMethod, ClassInfo targetClass,
                       TestCluster cluster) {
        return """
            Generate a JUnit 5 test method for the following Java class and method.
            The test should specifically exercise edge cases and boundary conditions.

            Target class:
            ```java
            %s
            ```

            Specific method to test: %s

            Available dependencies:
            %s

            Requirements:
            - Use JUnit 5 annotations (@Test)
            - Use static imports from org.junit.jupiter.api.Assertions
            - Include meaningful assertions
            - Focus on branches that are hard to reach
            - Handle potential exceptions

            Generate ONLY the test method body, no class wrapper.
            """.formatted(
                getClassSignature(targetClass),  // public methods, constructors
                getMethodSignature(uncoveredMethod),
                getDependencyInfo(cluster)
            );
    }
}
```

**What context to send**:
- Class signature (public API only — methods, constructors, fields)
- The specific uncovered method signature
- Available types on the classpath that can be used as parameters
- Do NOT send full source code (token cost, IP concerns)

### 11.3 Response Parsing

```java
public class ResponseParser {

    Optional<TestCase> parse(String llmResponse, TestCluster cluster) {
        // 1. Extract Java code from the LLM response (handle markdown fencing)
        String code = extractCode(llmResponse);

        // 2. Wrap in a compilable test class
        String fullClass = wrapInTestClass(code, cluster);

        // 3. Attempt to compile (in-memory using javax.tools.JavaCompiler)
        CompilationResult result = inMemoryCompile(fullClass);

        if (!result.success()) {
            return Optional.empty();  // will retry with feedback loop
        }

        // 4. Convert compiled test into internal TestCase representation
        //    by analyzing the bytecode of the compiled test
        return Optional.of(convertToTestCase(result.getBytecode(), cluster));
    }
}
```

### 11.4 Compilation Feedback Loop

If the LLM-generated test doesn't compile, feed errors back (TestSpark pattern):

```java
public class CompilationFeedback {
    int maxRetries = 3;

    Optional<TestCase> generateWithRetry(PromptBuilder promptBuilder,
                                          LLMClient llm, TestCluster cluster) {
        String prompt = promptBuilder.buildPrompt(...);
        String response = llm.query(prompt);

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Optional<TestCase> result = parser.parse(response, cluster);
            if (result.isPresent()) return result;

            // Get compilation errors
            List<String> errors = parser.getLastErrors();
            String fixPrompt = """
                The generated test did not compile. Errors:
                %s

                Please fix the test and regenerate.
                """.formatted(String.join("\n", errors));

            response = llm.query(fixPrompt);
        }

        return Optional.empty();  // give up after max retries
    }
}
```

### 11.5 Integration with DynaMOSA

```java
// Inside DynaMOSA.run(), add after target update:

if (codaMosa != null && codaMosa.isPlateaued()) {
    // Get uncovered goals
    Set<BranchCoverageGoal> uncovered = targets.getCurrentGoals().stream()
        .filter(g -> !archive.getCoveredGoals().contains(g))
        .collect(toSet());

    // Query LLM for each uncovered method (deduplicated)
    for (MethodInfo method : getMethodsForGoals(uncovered)) {
        Optional<TestCase> llmTest = codaMosa.generateTest(method, cluster);
        if (llmTest.isPresent()) {
            TestChromosome tc = new TestChromosome(llmTest.get());
            executor.execute(tc);
            evaluate(tc, targets.getCurrentGoals());
            archive.update(tc, tc.getFitnessValues());
            // Also inject into population for further evolution
            population.add(tc);
        }
    }
    codaMosa.onNewCoverage();  // reset plateau counter
}
```

### 11.6 LLM Client Abstraction

```java
public interface LLMClient {
    String query(String prompt);
    String query(String systemPrompt, String userPrompt);
    boolean isAvailable();
}

// Implementations for different providers:
// - OpenAIProvider (GPT-4, GPT-4o)
// - AnthropicProvider (Claude)
// - OllamaProvider (local models — Llama, CodeLlama, DeepSeek)
// - NoOpProvider (returns empty — used when LLM is disabled)
```

**Configuration**: LLM is disabled by default. Enable via config:
```json
{
  "llm": {
    "enabled": false,
    "provider": "openai",
    "model": "gpt-4o",
    "apiKey": "${JUNITISM_LLM_API_KEY}",
    "maxTokens": 2048,
    "plateauThreshold": 20
  }
}
```

---

## 12. Phase 9: Build Tool Integration & CLI

### 12.1 CLI (Picocli)

```
Usage: junitism [OPTIONS] --classpath <CP> --target <CLASS>

Required:
  -cp, --classpath <CP>      Classpath (JARs, directories) separated by : or ;
  -t,  --target <CLASS>      Target class name (e.g., com.example.OrderService)
                              Supports wildcards: com.example.*

Options:
  -o,  --output <DIR>        Output directory for generated tests
                              (default: ./junitism-tests)
  -b,  --budget <SECONDS>    Time budget per class (default: 60)
  --coverage-target <FLOAT>  Target branch coverage 0.0-1.0 (default: 0.9)
  --population <INT>         Population size (default: 50)
  --seed <LONG>              Random seed for reproducibility
  --source <DIR>             Optional source directory for richer analysis
  --llm                      Enable LLM augmentation (requires config)
  --report <FORMAT>          Report format: json, html, console (default: console)
  -v, --verbose              Verbose logging
  --config <FILE>            Configuration file path

Examples:
  junitism -cp target/classes:lib/*.jar -t com.example.OrderService
  junitism -cp app.jar -t "com.example.*" -o src/test/java -b 120
  junitism -cp build/libs/myapp.jar -t com.example.Service --llm
```

### 12.2 Maven Plugin

```xml
<plugin>
    <groupId>com.junitism</groupId>
    <artifactId>junitism-maven-plugin</artifactId>
    <version>${junitism.version}</version>
    <configuration>
        <targetPackages>
            <package>com.example.service</package>
            <package>com.example.model</package>
        </targetPackages>
        <excludes>
            <exclude>com.example.config.*</exclude>
        </excludes>
        <outputDirectory>${project.build.directory}/generated-test-sources</outputDirectory>
        <budget>60</budget>
        <coverageTarget>0.8</coverageTarget>
    </configuration>
</plugin>
```

Usage: `mvn junitism:generate`

### 12.3 Gradle Plugin

```groovy
plugins {
    id 'com.junitism' version '1.0.0'
}

junitism {
    targetPackages = ['com.example.service', 'com.example.model']
    excludes = ['com.example.config.*']
    budget = 60
    coverageTarget = 0.8
}
```

Usage: `gradle junitism`

### 12.4 Report Output

```json
{
  "summary": {
    "classesAnalyzed": 15,
    "totalBranches": 342,
    "coveredBranches": 291,
    "branchCoverage": 0.851,
    "testsGenerated": 87,
    "totalTimeSeconds": 412,
    "llmCallsMade": 12
  },
  "perClass": [
    {
      "className": "com.example.OrderService",
      "branches": 24,
      "covered": 22,
      "coverage": 0.917,
      "testsGenerated": 8,
      "timeSeconds": 34
    }
  ]
}
```

---

## 13. Phase 10: Enterprise Hardening

### 13.1 Handling Complex Object Construction

| Pattern | Strategy |
|---------|----------|
| **Builder pattern** | Detect builder methods (return `this` or builder type), chain them |
| **Factory methods** | Detect static methods returning the target type, prefer over constructors |
| **Dependency injection** | Construct concrete implementations of injected interfaces. If no concrete type found on classpath, generate a mock or use null |
| **Singleton** | Detect `getInstance()` pattern, use it instead of constructor |
| **Enums** | Enumerate all constants, use them in tests |
| **Records** | Use canonical constructor (auto-detected from Record attribute) |
| **Abstract classes** | Find concrete subclasses on classpath, instantiate those |
| **Interfaces** | Find implementing classes on classpath. If none: skip, null, or generate minimal anonymous implementation |

### 13.2 Handling Spring Boot / DI Classes

```java
public class DIAwareTestFactory extends TestFactory {
    // For classes with @Autowired constructors:
    // 1. Find the constructor parameters (they are injected dependencies)
    // 2. For each parameter type (typically an interface):
    //    a. Search classpath for concrete implementations
    //    b. If found: instantiate the concrete implementation
    //    c. If not found: create a mock object (Mockito.mock())
    // 3. Construct the target class with these dependencies

    // For @Value / @ConfigurationProperties:
    // Create a simple POJO with test values
}
```

Note: For full Spring context support (which would require starting a Spring container), this is out of scope for v1. The tool generates **unit tests**, not integration tests. Dependencies are constructed directly, not via Spring DI.

### 13.3 Handling Static Methods and Singletons

Static methods don't need object construction — call them directly:
```java
// PrimitiveStatement: var0:String = "hello"
// MethodStatement: var1:String = StringUtils.capitalize(var0)   ← static call
```

For singletons with private constructors: detect `getInstance()` and use it.

### 13.4 Multi-Class Generation

When targeting a package (`com.example.*`):

```java
public class MultiClassOrchestrator {
    void generateForPackage(String packagePattern, Path classpath) {
        // 1. Scan classpath for all classes matching pattern
        List<String> targetClasses = scanner.findClasses(packagePattern);

        // 2. Build dependency order (if A depends on B, generate B's tests first
        //    so B's behavior is established)
        List<String> ordered = topologicalSort(targetClasses);

        // 3. Generate tests class by class
        //    Optionally parallelize independent classes
        for (String className : ordered) {
            generateForClass(className);
        }

        // 4. Aggregate reports
    }
}
```

### 13.5 Parallel Generation

For independent classes, run DynaMOSA instances in parallel:

```java
ExecutorService pool = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors());

List<Future<GenerationResult>> futures = targetClasses.stream()
    .map(cls -> pool.submit(() -> generateForClass(cls)))
    .toList();
```

Each DynaMOSA instance uses `ThreadLocal` for `BranchTracker`, so parallel execution is safe.

### 13.6 Flaky Test Prevention

Generated tests must be deterministic:

1. **Re-run validation**: After generation, run each test 3 times. Drop tests that produce different results.
2. **Avoid time-dependent assertions**: Don't assert on `System.currentTimeMillis()`, `LocalDateTime.now()`, etc.
3. **Avoid order-dependent tests**: Each test method must be independent. No shared mutable state.
4. **Avoid hash-dependent assertions**: Don't assert on `hashCode()` (can vary across JVM runs for some types).
5. **Deterministic random seeds**: Use a configurable random seed so the same input always produces the same tests.

---

## 14. Branch Distance Formulas Reference

Complete table for all comparison types. Used by `BranchTracker`.

### Integer/Long Comparisons

| Predicate | True Distance (want true) | False Distance (want false) |
|-----------|--------------------------|----------------------------|
| `a == b` | `abs(a - b)` | `if a == b then 1 else 0` |
| `a != b` | `if a != b then 0 else 1` | `abs(a - b)` |
| `a < b` | `if a < b then 0 else (a - b + 1)` | `if a >= b then 0 else (b - a)` |
| `a <= b` | `if a <= b then 0 else (a - b)` | `if a > b then 0 else (b - a + 1)` |
| `a > b` | `if a > b then 0 else (b - a + 1)` | `if a <= b then 0 else (a - b)` |
| `a >= b` | `if a >= b then 0 else (b - a)` | `if a < b then 0 else (a - b + 1)` |

### Float/Double Comparisons

Same formulas as integer, but use floating-point subtraction. Handle `NaN` specially:
- Any comparison with `NaN` → distance = `Double.MAX_VALUE` (unreachable via numeric means)
- For `==`: `abs(a - b)` with epsilon-aware comparison

### Null Checks

| Predicate | True Distance | False Distance |
|-----------|--------------|----------------|
| `ref == null` (IFNULL) | `if null then 0 else 1` | `if null then 1 else 0` |
| `ref != null` (IFNONNULL) | `if not null then 0 else 1` | `if not null then 1 else 0` |

Binary: no gradient. The search must try different values.

### Reference Equality

| Predicate | True Distance | False Distance |
|-----------|--------------|----------------|
| `ref1 == ref2` (IF_ACMPEQ) | `if same then 0 else 1` | `if same then 1 else 0` |
| `ref1 != ref2` (IF_ACMPNE) | `if different then 0 else 1` | `if different then 1 else 0` |

Binary: no gradient.

### instanceof

| Predicate | True Distance | False Distance |
|-----------|--------------|----------------|
| `obj instanceof Type` | `if true then 0 else 1` | `if true then 1 else 0` |

Binary, but the search can try different types from the TestCluster.

### String Comparisons

When `String.equals()` is used (compiled as `INVOKEVIRTUAL` + `IFEQ`):

```java
double stringDistance(String a, String b) {
    if (a.equals(b)) return 0;
    // Levenshtein edit distance provides a gradient
    return levenshteinDistance(a, b);
}
```

This requires special handling: detect `String.equals()` calls in bytecode and insert a tracker that captures both string values. EvoSuite does this via method-level instrumentation of `equals()`, `compareTo()`, `startsWith()`, `endsWith()`, `contains()`.

### Switch Statements

For `TABLESWITCH(min, max, index)`:
```java
// Distance to matching case k:
double distance = abs(index - k);
// Distance to default case:
double defaultDist = (index >= min && index <= max) ? 1 : 0;
```

For `LOOKUPSWITCH(key, keys[])`:
```java
// Distance to matching key k:
double distance = abs(key - k);
// Minimum distance across all keys:
double minDist = Arrays.stream(keys).mapToDouble(k -> abs(key - k)).min();
```

### Normalization

All raw distances are normalized to [0, 1) before use in fitness:

```
normalize(d) = d / (d + 1.0)
```

This prevents large distance values from dominating the fitness landscape.

---

## 15. DynaMOSA Algorithm Reference

### Pseudocode (from Panichella et al., IEEE TSE 2018)

```
ALGORITHM DynaMOSA(Class C, Budget B)
  G ← extract all coverage goals from C
  CDG ← build control dependency graph
  current ← {g ∈ G | g has no structural dependency in CDG}  // root goals
  archive ← ∅
  P ← generateRandomPopulation(N)
  evaluateFitness(P, current)
  updateArchive(archive, P)
  activateChildren(current, archive, CDG)

  WHILE budget B not exhausted DO
    Q ← ∅
    WHILE |Q| < N DO
      parent1 ← tournamentSelection(P, current)
      parent2 ← tournamentSelection(P, current)
      (child1, child2) ← crossover(parent1, parent2)
      child1 ← mutate(child1)
      child2 ← mutate(child2)
      Q ← Q ∪ {child1, child2}
    END WHILE

    evaluateFitness(Q, current)
    updateArchive(archive, Q)
    activateChildren(current, archive, CDG)

    R ← P ∪ Q
    preferred ← preferenceSort(R, current)
    fronts ← fastNonDominatedSort(R, current)
    P ← selectSurvivors(fronts, preferred, N)
  END WHILE

  RETURN archive

FUNCTION activateChildren(current, archive, CDG)
  FOR EACH goal g newly covered in archive DO
    FOR EACH child c of g in CDG DO
      IF c not yet covered THEN
        current ← current ∪ {c}
      END IF
    END FOR
  END FOR

FUNCTION preferenceSort(R, current)
  preferred ← ∅
  FOR EACH goal g in current DO
    best ← argmin_{t ∈ R} fitness(t, g)
    preferred ← preferred ∪ {best}
  END FOR
  RETURN preferred
```

### Key Parameters

| Parameter | Default | Notes |
|-----------|---------|-------|
| Population size (N) | 50 | Higher = more diversity, slower |
| Crossover rate | 0.75 | Probability of applying crossover |
| Mutation rate | 1/L | L = average test length. Each statement has 1/L chance of mutation |
| Tournament size | 2 | Binary tournament selection |
| Max test length | 80 statements | Prevent bloated tests |
| Time budget | 60 seconds/class | Adjustable per configuration |
| Elitism | via preference criterion | Best test per goal always survives |

---

## 16. Configuration Parameters

All configurable via JSON config file, CLI flags, or Maven/Gradle properties:

```json
{
  "search": {
    "algorithm": "DYNAMOSA",
    "populationSize": 50,
    "crossoverRate": 0.75,
    "mutationRate": "auto",
    "maxTestLength": 80,
    "timeBudgetSeconds": 60,
    "stoppingCriteria": "TIME",
    "randomSeed": null,
    "tournamentSize": 2
  },
  "coverage": {
    "target": 0.9,
    "criteria": ["BRANCH"],
    "includePrivateMethods": false
  },
  "output": {
    "directory": "./junitism-tests",
    "packageMirroring": true,
    "variableNaming": "DESCRIPTIVE",
    "assertionStyle": "JUNIT5",
    "includeDisplayName": true,
    "codeFormatter": "GOOGLE_JAVA_FORMAT"
  },
  "execution": {
    "testTimeoutMs": 5000,
    "sandboxFileIO": true,
    "sandboxNetwork": true,
    "sandboxSystemExit": true
  },
  "analysis": {
    "classpath": [],
    "targetClasses": [],
    "excludeClasses": [],
    "sourceDirectory": null
  },
  "llm": {
    "enabled": false,
    "provider": "openai",
    "model": "gpt-4o",
    "apiKey": "${JUNITISM_LLM_API_KEY}",
    "maxTokens": 2048,
    "plateauThreshold": 20,
    "maxRetries": 3
  },
  "report": {
    "format": "console",
    "outputFile": null
  }
}
```

---

## 17. Testing Strategy for Junitism Itself

### Unit Tests

- **ClassAnalyzer**: Feed known `.class` bytecode, verify extracted methods/constructors/fields match expected
- **BranchInstrumentingVisitor**: Instrument a known class, verify probes are inserted at correct locations
- **BranchTracker**: Call tracking methods with known values, verify distances match formulas
- **TestCase representation**: Create/mutate/crossover test cases, verify type safety maintained
- **FitnessFunction**: Known execution data → verify fitness values match hand-calculated expected
- **NonDominatedSorting**: Known fitness vectors → verify fronts are correct
- **JUnit5Writer**: Generate code → compile it → verify it compiles and passes
- **Archive**: Feed known fitness values → verify correct tests are archived

### Integration Tests

Use a suite of test subject classes with known properties:

```java
// Simple: all branches reachable, no dependencies
public class Calculator {
    public int divide(int a, int b) { ... }
}

// Medium: multiple branches, object dependencies
public class OrderService {
    public OrderService(InventoryRepo repo) { ... }
    public boolean placeOrder(Order order) { ... }
}

// Hard: deep nesting, complex construction
public class BinarySearchTree<T extends Comparable<T>> {
    public void insert(T value) { ... }
    public boolean contains(T value) { ... }
    public void delete(T value) { ... }
}

// Edge cases: enums, records, sealed classes, static methods
public record Point(double x, double y) { ... }
public sealed interface Shape permits Circle, Rectangle { ... }
```

For each test subject:
1. Run Junitism against it
2. Verify generated tests compile
3. Verify generated tests pass
4. Measure branch coverage achieved
5. Regression: coverage should not decrease between Junitism versions

### Benchmarking

Use the SF110 corpus (110 open-source Java projects) or a subset for benchmarking:
- Measure coverage achieved vs. EvoSuite (on JDK 11 classes where EvoSuite still works)
- Measure generation time
- Measure test readability (subjective, sample-based)

---

## 18. Academic References

| Paper | Year | What to Take |
|-------|------|-------------|
| Panichella, Kifetew, Tonella — "Automated Test Case Generation as a Many-Objective Optimisation Problem with Dynamic Selection of the Targets" | IEEE TSE 2018 | **DynaMOSA algorithm** — the core search engine |
| Fraser & Arcuri — "Whole Test Suite Generation" | IEEE TSE 2013 | Whole-suite approach (fallback algorithm) |
| Lemieux & Sen — "CodaMosa: Escaping Coverage Plateaus in Test Generation with Pre-trained Large Language Models" | ICSE 2023 | **LLM hybrid pattern** — plateau escape |
| McMinn — "Search-Based Software Test Data Generation: A Survey" | SQJ 2004 | **Branch distance formulas**, fitness landscape theory |
| Deb et al. — "A Fast and Elitist Multiobjective Genetic Algorithm: NSGA-II" | IEEE TEC 2002 | **Non-dominated sorting** used in DynaMOSA |
| Arcuri & Briand — "A Hitchhiker's Guide to Statistical Tests for Assessing Randomized Algorithms in SE" | ICSE 2011 | How to **evaluate** the tool properly (Vargha-Delaney A12) |
| Pacheco & Ernst — "Randoop: Feedback-Directed Random Test Generation" | 2007 | Feedback-directed random generation (Phase 1 baseline) |
| Lukasczyk, Kroiß, Fraser — "An Empirical Study of Automated Unit Test Generation for Python" | EMSE 2023 | **Pynguin**: clean DynaMOSA reference implementation |
| Schafer et al. — "An Empirical Evaluation of Using Large Language Models for Automated Unit Test Generation" | IEEE TSE 2024 | LLM test generation evaluation, compilation feedback loops |

---

## Implementation Timeline

| Phase | Description | Duration | Dependencies | Exit Criteria |
|-------|-------------|----------|--------------|--------------|
| 1 | Class Analysis & TestCluster | 3-4 weeks | None | Parse any JDK 17 JAR, build complete TestCluster |
| 2 | Bytecode Instrumentation | 3-4 weeks | Phase 1 | Branch probes working, distances reported correctly |
| 3 | Test Case Representation & Operators | 2-3 weeks | Phase 1 | Create, mutate, crossover tests maintaining type safety |
| 4 | DynaMOSA Search Engine | 4-5 weeks | Phases 1-3 | DynaMOSA runs, achieves >80% coverage on Calculator-level classes |
| 5 | Sandboxed Execution | 2 weeks | Phase 2 | Safe execution with timeouts, no System.exit crashes |
| 6 | Assertion Generation | 2-3 weeks | Phases 4-5 | Meaningful assertions, regression-stable |
| 7 | JUnit 5 Code Output | 2 weeks | Phase 6 | Compilable, readable, passing JUnit 5 test files |
| 8 | CodaMosa LLM Layer | 3 weeks | Phase 4 | Plateau escape working, measurable coverage improvement |
| 9 | CLI & Build Plugins | 2-3 weeks | Phase 7 | `junitism` CLI works, Maven/Gradle plugins work |
| 10 | Enterprise Hardening | 3-4 weeks | All above | Handles Spring classes, complex hierarchies, parallel generation |

**Total: ~7-8 months with 2-3 engineers, or ~4-5 months with LLM-agentic acceleration.**

Phase 1-5 can start producing working (basic) tests by month 2-3. Each subsequent phase adds quality and capability.

---

## Quick Start: What to Build First

If you're using LLM-agentic coding to accelerate this, start here in order:

1. **Set up the multi-module Maven project** (Section 2)
2. **Implement ClassAnalyzer** (Section 4.1) — read a JAR, extract methods
3. **Implement TestCluster** (Section 4.2) — catalog types and constructors
4. **Implement TestCase + Statements** (Section 6.1-6.3) — internal representation
5. **Implement TestFactory** (Section 6.7) — generate random test cases
6. **Implement basic TestExecutor** (Section 8.2) — run tests via reflection
7. **Implement JUnit5Writer** (Section 10.1) — output .java files

This gives you a working **random test generator** (Randoop-style) end-to-end. Then layer on:

8. **BranchInstrumentingVisitor + BranchTracker** (Section 5.2-5.3)
9. **CFG + CDG construction** (Section 5.4-5.5)
10. **FitnessFunction** (Section 7.5)
11. **DynaMOSA** (Section 7.7)

Now you have coverage-guided generation. Then add quality:

12. **Assertion generation** (Section 9)
13. **CodaMosa** (Section 11)
14. **CLI + plugins** (Section 12)
