package com.thiyagarajan.agent.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.runtime.DataDrivenExecutionResult;
import com.thiyagarajan.agent.runtime.ExecutionResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class DataDrivenReportManagerTest {
    @Test
    void writesHtmlJsonAndCsvAndRedactsSensitiveDatasetValues() throws Exception {
        Path dir = Files.createTempDirectory("data-driven-report-");
        DataDrivenExecutionResult result = new DataDrivenExecutionResult();
        result.testName = "Login data driven";
        result.totalIterations = 1;
        result.passedIterations = 0;
        result.failedIterations = 1;
        result.durationMs = 25;

        ExecutionResult execution = new ExecutionResult();
        execution.testName = result.testName;
        execution.passed = false;
        execution.failureAnalysis("password=secret-value");
        execution.steps.add(new ExecutionResult.StepResult("POST", false, "HTTP 401", 25));

        DataDrivenExecutionResult.IterationResult iteration = new DataDrivenExecutionResult.IterationResult();
        iteration.index = 1;
        iteration.passed = false;
        iteration.durationMs = 25;
        iteration.data = new LinkedHashMap<>();
        iteration.data.put("username", "demo");
        iteration.data.put("password", "secret-value");
        iteration.execution = execution;
        result.iterations.add(iteration);

        Path html = new DataDrivenReportManager(new ObjectMapper(), dir).writeAll(result);
        assertTrue(Files.isRegularFile(html));
        assertTrue(Files.isRegularFile(dir.resolve("data-driven-report.json")));
        assertTrue(Files.isRegularFile(dir.resolve("data-driven-report.csv")));

        String htmlText = Files.readString(html);
        String jsonText = Files.readString(dir.resolve("data-driven-report.json"));
        String csvText = Files.readString(dir.resolve("data-driven-report.csv"));
        assertTrue(htmlText.contains("Login data driven"));
        assertTrue(htmlText.contains("FAIL"));
        assertFalse(jsonText.contains("secret-value"));
        assertFalse(csvText.contains("secret-value"));
    }
}
