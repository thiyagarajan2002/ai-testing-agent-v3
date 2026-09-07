package com.thiyagarajan.agent.runtime;

import java.util.ArrayList;
import java.util.List;

/** Aggregate result for a parameterized/data-driven execution. */
public class DataDrivenExecutionResult {
    public String testName;
    public int totalIterations;
    public int passedIterations;
    public int failedIterations;
    public long durationMs;
    public String executionMode = "SEQUENTIAL";
    public int parallelism = 1;
    public long estimatedSequentialDurationMs;
    public double estimatedSpeedup = 1.0;
    public List<IterationResult> iterations = new ArrayList<>();

    public boolean passed() { return failedIterations == 0 && totalIterations > 0; }

    public double averageIterationDurationMs() {
        return totalIterations == 0 ? 0.0 : estimatedSequentialDurationMs * 1.0 / totalIterations;
    }

    public static class IterationResult {
        public int index;
        public java.util.Map<String, String> data;
        public boolean passed;
        public long durationMs;
        public ExecutionResult execution;
    }
}
