package com.junitism.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.HashMap;
import java.util.Map;

/**
 * Coverage and generation reports.
 */
public class ReportGenerator {

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public String toJson(Map<String, Object> report) {
        try {
            return mapper.writeValueAsString(report);
        } catch (Exception e) {
            return "{}";
        }
    }

    public Map<String, Object> createSummary(int classesAnalyzed, int testsGenerated, double coverage) {
        Map<String, Object> report = new HashMap<>();
        report.put("classesAnalyzed", classesAnalyzed);
        report.put("testsGenerated", testsGenerated);
        report.put("branchCoverage", coverage);
        return report;
    }
}
