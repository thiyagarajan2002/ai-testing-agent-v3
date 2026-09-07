package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryAnalyticsManagerTest {
    @TempDir Path temp;

    @Test
    void detectsAlternatingTestAsFlakyAndWritesDashboards() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        Path history = temp.resolve("history");
        RunHistoryManager manager = new RunHistoryManager(history, mapper);

        for (int i = 0; i < 4; i++) {
            SuiteExecutionResult suite = suite(i, i % 2 == 0 ? "PASS" : "FAIL");
            manager.recordSuite(suite);
        }

        HistoryAnalyticsManager analytics = new HistoryAnalyticsManager(history, mapper);
        HistoryAnalyticsManager.AnalyticsReport report = analytics.writeReports(10, 0.50d);

        assertEquals(4, report.runsAnalyzed);
        assertEquals(1, report.totalTestsObserved);
        assertEquals(1, report.flakyTests.size());
        HistoryAnalyticsManager.TestStats stat = report.flakyTests.get(0);
        assertEquals(4, stat.executions);
        assertEquals(2, stat.passed);
        assertEquals(2, stat.failed);
        assertEquals(3, stat.statusChanges);
        assertEquals(1.0d, stat.flakinessRate());
        assertTrue(java.nio.file.Files.isRegularFile(history.resolve("analytics.html")));
        assertTrue(java.nio.file.Files.isRegularFile(history.resolve("analytics.json")));
        assertTrue(java.nio.file.Files.isRegularFile(history.resolve("analytics.csv")));
    }

    @Test
    void stablePassingTestIsNotFlaky() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        Path history = temp.resolve("history");
        RunHistoryManager manager = new RunHistoryManager(history, mapper);
        for (int i = 0; i < 3; i++) manager.recordSuite(suite(i, "PASS"));

        HistoryAnalyticsManager.AnalyticsReport report = new HistoryAnalyticsManager(history, mapper).analyze(10, 0.50d);
        assertTrue(report.flakyTests.isEmpty());
        assertEquals(0, report.flakyTests.size());
    }

    private SuiteExecutionResult suite(int run, String status) {
        SuiteExecutionResult suite = new SuiteExecutionResult();
        suite.suiteName = "Analytics Suite";
        suite.status = "FAIL".equals(status) ? "FAILED" : "PASSED";
        suite.totalTests = 1;
        suite.passedTests = "PASS".equals(status) ? 1 : 0;
        suite.failedTests = "FAIL".equals(status) ? 1 : 0;
        suite.durationMs = 100 + run;
        ExecutionResult result = new ExecutionResult();
        result.testName = "Login API";
        result.passed = "PASS".equals(status);
        suite.tests = List.of(new SuiteExecutionResult.TestExecution(0, "plans/login.json", "Login API", status, 100 + run, result));
        return suite;
    }
}
