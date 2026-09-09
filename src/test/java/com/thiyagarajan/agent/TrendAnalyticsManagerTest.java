package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.runtime.RunHistoryManager;
import com.thiyagarajan.agent.runtime.SuiteExecutionResult;
import com.thiyagarajan.agent.runtime.TrendAnalyticsManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TrendAnalyticsManagerTest {
    @Test
    void calculatesPassRateDurationAndRegressionTrend() throws Exception {
        Path root = Files.createTempDirectory("trend-test-");
        RunHistoryManager history = new RunHistoryManager(root, new ObjectMapper());
        history.recordSuite(suite("PASS", 1, 1, 0, 100));
        history.recordSuite(suite("FAILED", 1, 0, 1, 250));

        var report = new TrendAnalyticsManager(root).analyze(20);

        assertEquals(2, report.runsAnalyzed);
        assertEquals(-100.0, report.passRateDelta, 0.001);
        assertEquals(150, report.durationDeltaMs);
        assertEquals(1, report.regressionTotal);
        assertEquals(250, report.peakDurationMs);
        assertEquals(2, report.points.size());
    }

    @Test
    void writesTrendArtifacts() throws Exception {
        Path root = Files.createTempDirectory("trend-write-");
        RunHistoryManager history = new RunHistoryManager(root, new ObjectMapper());
        history.recordSuite(suite("PASS", 1, 1, 0, 100));

        new TrendAnalyticsManager(root).writeReports(10);

        assertTrue(Files.isRegularFile(root.resolve("trends.json")));
        assertTrue(Files.isRegularFile(root.resolve("trends.csv")));
        assertTrue(Files.isRegularFile(root.resolve("trends.html")));
    }

    private SuiteExecutionResult suite(String status, int total, int passed, int failed, long duration) {
        SuiteExecutionResult r = new SuiteExecutionResult();
        r.suiteName = "trend-suite";
        r.status = status;
        r.totalTests = total;
        r.passedTests = passed;
        r.failedTests = failed;
        r.durationMs = duration;
        r.tests = new java.util.ArrayList<>();
        SuiteExecutionResult.TestExecution t = new SuiteExecutionResult.TestExecution();
        t.planFile = "plan.json";
        t.testName = "login";
        t.status = passed > 0 ? "PASS" : "FAIL";
        t.durationMs = duration;
        r.tests.add(t);
        return r;
    }
}
