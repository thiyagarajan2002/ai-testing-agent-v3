package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.runtime.RunHistoryManager;
import com.thiyagarajan.agent.runtime.RunHistoryManager.RunSummary;
import com.thiyagarajan.agent.runtime.RunComparisonResult;
import com.thiyagarajan.agent.runtime.SuiteExecutionResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RunHistoryManagerTest {
    @Test
    void recordsSuiteAndCreatesIndex() throws Exception {
        Path root = Files.createTempDirectory("history-test-");
        RunHistoryManager manager = new RunHistoryManager(root, new ObjectMapper());
        SuiteExecutionResult suite = suite("PASS", 1, 1, 0);

        var recorded = manager.recordSuite(suite);

        assertTrue(Files.isRegularFile(root.resolve("index.json")));
        assertTrue(Files.isRegularFile(recorded.directory().resolve("suite-execution.json")));
        assertTrue(Files.isRegularFile(recorded.directory().resolve("comparison.json")));
        List<RunSummary> index = new ObjectMapper().readValue(root.resolve("index.json").toFile(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
        assertEquals(1, index.size());
        assertEquals(recorded.runId(), index.get(0).runId);
    }

    @Test
    void detectsRegressionAndFixedTest() throws Exception {
        Path root = Files.createTempDirectory("history-compare-");
        RunHistoryManager manager = new RunHistoryManager(root, new ObjectMapper());
        SuiteExecutionResult previous = suiteWithTest("PASS");
        SuiteExecutionResult current = suiteWithTest("FAIL");
        RunComparisonResult comparison = manager.compare("old-run", previous, "new-run", current);
        assertEquals(1, comparison.regressions);
        assertEquals(0, comparison.fixed);

        comparison = manager.compare("old-run", current, "new-run", previous);
        assertEquals(0, comparison.regressions);
        assertEquals(1, comparison.fixed);
    }

    private SuiteExecutionResult suite(String status, int total, int passed, int failed) {
        SuiteExecutionResult r = new SuiteExecutionResult();
        r.suiteName = "history-suite";
        r.status = status;
        r.totalTests = total;
        r.passedTests = passed;
        r.failedTests = failed;
        r.tests = new java.util.ArrayList<>();
        return r;
    }

    private SuiteExecutionResult suiteWithTest(String status) {
        SuiteExecutionResult r = suite("FAIL".equals(status) ? "FAILED" : "PASSED", 1, "PASS".equals(status) ? 1 : 0, "FAIL".equals(status) ? 1 : 0);
        SuiteExecutionResult.TestExecution t = new SuiteExecutionResult.TestExecution();
        t.planFile = "plan.json";
        t.testName = "login";
        t.status = status;
        r.tests.add(t);
        return r;
    }
}
