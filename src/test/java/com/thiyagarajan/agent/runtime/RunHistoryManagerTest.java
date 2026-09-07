package com.thiyagarajan.agent.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RunHistoryManagerTest {
    @TempDir Path tempDir;

    @Test
    void comparesRegressionFixedNewAndRemovedTests() throws Exception {
        RunHistoryManager history = new RunHistoryManager(tempDir.resolve("history"));

        SuiteExecutionResult previous = suite("Smoke", 300,
                test(0, "a.json", "A", "PASS", 100),
                test(1, "b.json", "B", "FAIL", 120),
                test(2, "removed.json", "Removed", "PASS", 80));
        RunHistoryManager.RecordedRun first = history.recordSuite(previous);
        assertEquals(3, first.comparison().newTests);

        SuiteExecutionResult current = suite("Smoke", 340,
                test(0, "a.json", "A", "FAIL", 150),
                test(1, "b.json", "B", "PASS", 90),
                test(2, "new.json", "New", "PASS", 100));
        RunHistoryManager.RecordedRun second = history.recordSuite(current);

        assertEquals(1, second.comparison().regressions);
        assertEquals(1, second.comparison().fixed);
        assertEquals(1, second.comparison().newTests);
        assertEquals(1, second.comparison().removedTests);
        assertEquals(40, second.comparison().durationDeltaMs);
        assertTrue(Files.isRegularFile(second.directory().resolve("comparison.json")));
        assertTrue(Files.isRegularFile(second.directory().resolve("comparison.csv")));
        assertTrue(Files.isRegularFile(second.directory().resolve("comparison.html")));
        assertTrue(Files.isRegularFile(tempDir.resolve("history/index.json")));
        assertTrue(Files.isRegularFile(tempDir.resolve("history/index.html")));
    }

    @Test
    void rejectsDuplicateStableTestIdentity() throws Exception {
        RunHistoryManager history = new RunHistoryManager(tempDir.resolve("duplicates"));
        SuiteExecutionResult previous = suite("Smoke", 10, test(0, "same.json", "Same", "PASS", 10));
        history.recordSuite(previous);
        SuiteExecutionResult current = suite("Smoke", 20,
                test(0, "same.json", "Same", "PASS", 10),
                test(1, "same.json", "Same", "PASS", 10));
        assertThrows(IllegalArgumentException.class, () -> history.recordSuite(current));
    }

    private SuiteExecutionResult suite(String name, long duration, SuiteExecutionResult.TestExecution... tests) {
        SuiteExecutionResult suite = new SuiteExecutionResult();
        suite.suiteName = name;
        suite.durationMs = duration;
        for (var test : tests) suite.tests.add(test);
        suite.totalTests = suite.tests.size();
        suite.passedTests = (int) suite.tests.stream().filter(t -> "PASS".equals(t.status)).count();
        suite.failedTests = suite.totalTests - suite.passedTests;
        suite.status = suite.failedTests == 0 ? "PASS" : "FAIL";
        suite.completedAt = java.time.Instant.now().toString();
        return suite;
    }

    private SuiteExecutionResult.TestExecution test(int index, String plan, String name, String status, long duration) {
        ExecutionResult result = new ExecutionResult();
        result.testName = name;
        result.passed = "PASS".equals(status);
        return new SuiteExecutionResult.TestExecution(index, plan, name, status, duration, result);
    }
}
