package com.thiyagarajan.agent.runtime;

import java.util.ArrayList;
import java.util.List;

public class ExecutionResult {
    public String testName;
    public boolean passed;
    public String failureAnalysis = "";
    public List<StepResult> steps = new ArrayList<>();

    public boolean passed() { return passed; }
    public String failureAnalysis() { return failureAnalysis; }
    public void failureAnalysis(String value) { this.failureAnalysis = value; }

    public static class StepResult {
        public String action;
        public boolean passed;
        public String details;
        public long durationMs;

        public StepResult(String action, boolean passed, String details, long durationMs) {
            this.action = action;
            this.passed = passed;
            this.details = details;
            this.durationMs = durationMs;
        }
    }
}
