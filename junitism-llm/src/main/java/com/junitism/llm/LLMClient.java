package com.junitism.llm;

/**
 * Abstract LLM API client.
 */
public interface LLMClient {

    String query(String prompt);

    String query(String systemPrompt, String userPrompt);

    boolean isAvailable();
}
