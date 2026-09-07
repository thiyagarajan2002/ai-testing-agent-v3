package com.thiyagarajan.agent.runtime;

import java.util.ArrayList;
import java.util.List;

/** Comparison between the current suite run and the immediately previous recorded run. */
public class RunComparisonResult {
    public String previousRunId;
    public String currentRunId;
    public int previousTotalTests;
    public int currentTotalTests;
    public int previousPassedTests;
    public int currentPassedTests;
    public int previousFailedTests;
    public int currentFailedTests;
    public double previousPassRate;
    public double currentPassRate;
    public double passRateDelta;
    public long previousDurationMs;
    public long currentDurationMs;
    public long durationDeltaMs;
    public int regressions;
    public int fixed;
    public int unchangedPassed;
    public int unchangedFailed;
    public int newTests;
    public int removedTests;
    public List<TestChange> changes = new ArrayList<>();

    public boolean hasRegression() { return regressions > 0; }

    public static class TestChange {
        public String key;
        public String planFile;
        public String testName;
        public String previousStatus;
        public String currentStatus;
        public String category;
        public long previousDurationMs;
        public long currentDurationMs;
        public long durationDeltaMs;

        public TestChange() { }

        public TestChange(String key, String planFile, String testName, String previousStatus, String currentStatus,
                          String category, long previousDurationMs, long currentDurationMs) {
            this.key = key;
            this.planFile = planFile;
            this.testName = testName;
            this.previousStatus = previousStatus;
            this.currentStatus = currentStatus;
            this.category = category;
            this.previousDurationMs = previousDurationMs;
            this.currentDurationMs = currentDurationMs;
            this.durationDeltaMs = currentDurationMs - previousDurationMs;
        }
    }
}
