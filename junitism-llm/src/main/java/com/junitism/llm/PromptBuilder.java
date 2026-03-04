package com.junitism.llm;

import com.junitism.analysis.MethodInfo;
import com.junitism.analysis.TestCluster;

/**
 * Build prompts from uncovered targets for LLM test generation.
 */
public class PromptBuilder {

    public String buildPrompt(MethodInfo uncoveredMethod, TestCluster.ClassInfo targetClass, TestCluster cluster) {
        return """
            Generate a JUnit 5 test method for the following Java class and method.
            The test should specifically exercise edge cases and boundary conditions.

            Target class: %s
            Method to test: %s

            Requirements:
            - Use JUnit 5 annotations (@Test)
            - Use static imports from org.junit.jupiter.api.Assertions
            - Include meaningful assertions
            - Focus on branches that are hard to reach

            Generate ONLY the test method body, no class wrapper.
            """.formatted(
                targetClass.getType().getBinaryName(),
                uncoveredMethod.getSignature()
        );
    }
}
