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
    public List<IterationResult> iterations = new ArrayList<>();

    public boolean passed() { return failedIterations == 0 && totalIterations > 0; }

    public static class IterationResult {
        public int index;
        public java.util.Map<String, String> data;
        public boolean passed;
        public long durationMs;
        public ExecutionResult execution;
    }
}
