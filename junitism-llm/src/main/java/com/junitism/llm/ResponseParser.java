package com.junitism.llm;

import com.junitism.analysis.TestCluster;
import com.junitism.testcase.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse LLM response to extract Java code and convert to TestCase.
 */
public class ResponseParser {

    private List<String> lastErrors = new ArrayList<>();

    public Optional<TestCase> parse(String llmResponse, TestCluster cluster) {
        lastErrors.clear();
        String code = extractCode(llmResponse);
        if (code == null || code.isBlank()) {
            lastErrors.add("No code found in response");
            return Optional.empty();
        }
        return Optional.empty();
    }

    private String extractCode(String response) {
        Pattern p = Pattern.compile("```(?:java)?\\s*([\\s\\S]*?)```");
        Matcher m = p.matcher(response);
        return m.find() ? m.group(1).trim() : response.trim();
    }

    public List<String> getLastErrors() {
        return lastErrors;
    }
}
