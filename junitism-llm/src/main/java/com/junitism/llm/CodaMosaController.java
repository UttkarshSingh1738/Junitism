package com.junitism.llm;

/**
 * Plateau detection for DynaMOSA - triggers LLM when no new coverage for N generations.
 */
public class CodaMosaController {

    private final int plateauThreshold;
    private int generationsSinceLastCoverage = 0;

    public CodaMosaController() {
        this(20);
    }

    public CodaMosaController(int plateauThreshold) {
        this.plateauThreshold = plateauThreshold;
    }

    public boolean isPlateaued() {
        return generationsSinceLastCoverage >= plateauThreshold;
    }

    public void onNewCoverage() {
        generationsSinceLastCoverage = 0;
    }

    public void onGenerationComplete() {
        generationsSinceLastCoverage++;
    }
}
