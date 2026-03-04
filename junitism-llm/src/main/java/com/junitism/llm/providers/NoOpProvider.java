package com.junitism.llm.providers;

import com.junitism.llm.LLMClient;

/**
 * No-op provider when LLM is disabled.
 */
public class NoOpProvider implements LLMClient {

    @Override
    public String query(String prompt) {
        return "";
    }

    @Override
    public String query(String systemPrompt, String userPrompt) {
        return "";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
