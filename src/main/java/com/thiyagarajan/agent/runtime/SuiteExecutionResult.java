package com.thiyagarajan.agent.runtime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Aggregated result for a complete test suite execution. */
public class SuiteExecutionResult {
    public String suiteName;
    public String status;
    public String startedAt;
    public String completedAt;
    public long durationMs;
    public int totalTests;
    public int passedTests;
    public int failedTests;
    public List<TestExecution> tests = new ArrayList<>();

    public SuiteExecutionResult() {
        startedAt = Instant.now().toString();
    }

    public boolean passed() {
        return failedTests == 0 && totalTests > 0;
    }

    public static class TestExecution {
        public int index;
        public String planFile;
        public String testName;
        public String status;
        public long durationMs;
        public ExecutionResult result;

        public TestExecution() { }

        public TestExecution(int index, String planFile, String testName,
                             String status, long durationMs, ExecutionResult result) {
            this.index = index;
            this.planFile = planFile;
            this.testName = testName;
            this.status = status;
            this.durationMs = durationMs;
            this.result = result;
        }
    }
}
