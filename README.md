# Junitism

Automated JUnit 5 Test Suite Generator for JDK 17+ using DynaMOSA (search-based algorithm), ASM/ByteBuddy for bytecode analysis and instrumentation, and JavaParser for code output.

## Build

```bash
mvn clean install
```

## Usage

```bash
java -jar junitism-cli/target/junitism-cli-1.0.0-SNAPSHOT.jar -cp <classpath> -t <target-class>
```

## Modules

- **junitism-runtime**: Runtime support for instrumented execution
- **junitism-core**: Core engine (analysis, instrumentation, DynaMOSA)
- **junitism-llm**: Optional CodaMosa LLM augmentation
- **junitism-cli**: Command-line interface
- **junitism-maven-plugin**: Maven plugin (`mvn junitism:generate`)
- **junitism-gradle-plugin**: Gradle plugin
- **junitism-tests**: Integration tests and test subjects
