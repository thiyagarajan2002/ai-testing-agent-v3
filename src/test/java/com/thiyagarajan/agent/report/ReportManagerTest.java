package com.thiyagarajan.agent.report;

import com.thiyagarajan.agent.runtime.ExecutionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void writeAllGeneratesOnlyHtmlCsvAndPdfReports() throws Exception {
        ExecutionResult result = new ExecutionResult();
        result.testName = "report-regression";
        result.passed = false;
        result.failureAnalysis = "Expected status 200 but received 500";
        result.steps.add(new ExecutionResult.StepResult(
                "GET /users",
                true,
                "Request: GET /users\nResponse: 200 OK",
                120,
                List.of("reports/api/logs/users.log")));
        result.steps.add(new ExecutionResult.StepResult(
                "assert status",
                false,
                "Expected: 200\nActual: 500",
                80));

        new ReportManager(tempDir).writeAll(result);

        Set<String> files;
        try (var stream = Files.list(tempDir)) {
            files = stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        }

        assertEquals(Set.of("report.html", "report.csv", "report.pdf"), files);
        assertTrue(Files.size(tempDir.resolve("report.pdf")) > 0);

        String html = Files.readString(tempDir.resolve("report.html"), StandardCharsets.UTF_8);
        assertTrue(html.contains("AI Testing Agent — Execution Report"));
        assertTrue(html.contains("Pass rate"));
        assertTrue(html.contains("report.csv"));
        assertTrue(html.contains("report.pdf"));
        assertTrue(html.contains("Expected: 200"));
        assertTrue(html.contains("reports/api/logs/users.log"));
        assertFalse(html.contains("execution.json"));
        assertFalse(html.contains("execution.log"));

        String csv = Files.readString(tempDir.resolve("report.csv"), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\ufeff"));
        assertTrue(csv.contains("SUMMARY"));
        assertTrue(csv.contains("Failure Analysis"));
        assertTrue(csv.contains("Expected status 200 but received 500"));
    }
}
