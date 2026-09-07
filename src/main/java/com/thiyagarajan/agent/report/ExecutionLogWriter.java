package com.thiyagarajan.agent.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.runtime.ExecutionResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writes a machine-readable execution artifact alongside human reports. */
public final class ExecutionLogWriter {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public void write(ExecutionResult result, Path directory) throws Exception {
        Files.createDirectories(directory);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("generatedAt", OffsetDateTime.now().toString());
        root.put("testName", result.testName);
        root.put("status", result.passed ? "PASS" : "FAIL");
        root.put("failureAnalysis", result.failureAnalysis);
        root.put("stepCount", result.steps.size());
        root.put("passedSteps", result.steps.stream().filter(s -> s.passed).count());
        root.put("failedSteps", result.steps.stream().filter(s -> !s.passed).count());
        root.put("steps", result.steps);
        mapper.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("execution.json").toFile(), root);

        StringBuilder log = new StringBuilder();
        log.append("AI Testing Agent Execution Log\n");
        log.append("Generated: ").append(root.get("generatedAt")).append('\n');
        log.append("Test: ").append(result.testName).append('\n');
        log.append("Status: ").append(root.get("status")).append('\n');
        log.append("Steps: ").append(result.steps.size()).append('\n');
        for (int i = 0; i < result.steps.size(); i++) {
            var step = result.steps.get(i);
            log.append('\n').append("Step ").append(i + 1).append('\n');
            log.append("Action: ").append(step.action).append('\n');
            log.append("Status: ").append(step.passed ? "PASS" : "FAIL").append('\n');
            log.append("DurationMs: ").append(step.durationMs).append('\n');
            log.append("Details: ").append(step.details).append('\n');
        }
        if (result.failureAnalysis != null && !result.failureAnalysis.isBlank()) {
            log.append("\nFailure Analysis:\n").append(result.failureAnalysis).append('\n');
        }
        Files.writeString(directory.resolve("execution.log"), log.toString());
    }
}
